import API, { Settings } from "api"
import { gql } from "apollo-boost"
import SVGCanvas from "components/graphs/SVGCanvas"
import {
  PageDispatchersPropType,
  mapPageDispatchersToProps,
  useBoilerplate
} from "components/Page"
import * as d3 from "d3"
import _xor from "lodash/xor"
import { Symbol } from "milsymbol"
import { Organization, Position, Person } from "models"
import PropTypes from "prop-types"
import React, { useEffect, useLayoutEffect, useRef, useState } from "react"
import { connect } from "react-redux"
import { useHistory } from "react-router-dom"
import DEFAULT_AVATAR from "resources/default_avatar.svg"
import EMPTY_SET from "resources/empty_set.svg"
import COLLAPSE_ICON from "resources/organizations.png"
import EXPAND_ICON from "resources/plus.png"

const app6IconSize = 28
const avatarSize = 32

const GQL_GET_CHART_DATA = gql`
  query($uuid: String!) {
    organization(uuid: $uuid) {
      uuid
      shortName
      longName
      type
      positions {
        name
        uuid
        person {
          rank
          name
          uuid
          avatar(size: ${avatarSize})
        }
      }
      childrenOrgs(query: { pageNum: 0, pageSize: 0, status: ACTIVE }) {
        uuid
      }
      descendantOrgs(query: { pageNum: 0, pageSize: 0, status: ACTIVE }) {
        uuid
        shortName
        longName
        type
        childrenOrgs(query: { pageNum: 0, pageSize: 0, status: ACTIVE }) {
          uuid
        }
        parentOrg {
          uuid
        }
        positions {
          name
          uuid
          person {
            rank
            name
            uuid
            avatar(size: 32)
          }
        }
      }
    }
  }
`
const transitionDuration = 200

const ranks = Settings.fields.person.ranks.map(rank => rank.value)

const sortPositions = (positions, truncateLimit) => {
  const allResults = [...positions].sort((p1, p2) =>
    ranks.indexOf(p1.person?.rank) > ranks.indexOf(p2.person?.rank) ? -1 : 1
  )
  return truncateLimit !== undefined && truncateLimit < allResults.length
    ? allResults.slice(0, truncateLimit)
    : allResults
}

// TODO: enable once innerhtml in svg is polyfilled
// const EXPAND_ICON = renderBlueprintIconAsSvg(IconNames.DIAGRAM_TREE)
// const COLLAPSE_ICON = renderBlueprintIconAsSvg(IconNames.CROSS)

const OrganizationalChart = ({
  pageDispatchers,
  org,
  width,
  height: initialHeight
}) => {
  const [expanded, setExpanded] = useState([])
  const [personnelDepth, setPersonnelDepth] = useState(9)
  const history = useHistory()
  const canvasRef = useRef(null)
  const svgRef = useRef(null)
  const linkRef = useRef(null)
  const nodeRef = useRef(null)
  const tree = useRef(d3.tree())
  const [root, setRoot] = useState(null)
  const [height, setHeight] = useState(initialHeight)
  const nodeSize = [105, 100 + 11 * 5]
  const { loading, error, data } = API.useApiQuery(GQL_GET_CHART_DATA, {
    uuid: org.uuid
  })

  const { done, result } = useBoilerplate({
    loading,
    error,
    pageDispatchers
  })

  const canvas = d3.select(canvasRef.current)
  const link = d3.select(linkRef.current)
  const node = d3.select(nodeRef.current)

  useEffect(() => {
    data &&
      setRoot(
        d3.hierarchy(data.organization, d =>
          expanded.includes(d.uuid)
            ? data.organization.descendantOrgs.filter(
              org => org.parentOrg?.uuid === d.uuid
            )
            : null
        )
      )
  }, [data, expanded])

  useEffect(() => {
    if (!data || !root) {
      return
    }

    const calculateBounds = rootArg => {
      const boundingBox = rootArg.descendants().reduce(
        (box, nodeArg) => {
          return {
            xmin: Math.min(box.xmin, nodeArg.x || 0),
            xmax: Math.max(box.xmax, nodeArg.x || 0),
            ymin: Math.min(box.ymin, nodeArg.y || 0),
            ymax: Math.max(box.ymax, nodeArg.y || 0)
          }
        },
        {
          xmin: Number.MAX_SAFE_INTEGER,
          xmax: Number.MIN_SAFE_INTEGER,
          ymin: Number.MAX_SAFE_INTEGER,
          ymax: Number.MIN_SAFE_INTEGER
        }
      )
      return {
        box: boundingBox,
        size: [
          boundingBox.xmax - boundingBox.xmin + nodeSize[0],
          boundingBox.ymax - boundingBox.ymin + nodeSize[1]
        ],
        center: [
          (boundingBox.xmax + boundingBox.xmin + nodeSize[0] - 50) / 2,
          (boundingBox.ymax + boundingBox.ymin + nodeSize[1] - 50) / 2
        ]
      }
    }

    tree.current.nodeSize(nodeSize)
    const bounds = calculateBounds(root)
    const scale = Math.min(
      1.2,
      1 / Math.max(bounds.size[0] / width, bounds.size[1] / height)
    )
    canvas.attr(
      "transform",
      `translate(${width / 2 - scale * bounds.center[0]},${height / 2 -
        scale * bounds.center[1] +
        80}) scale(${scale})`
    )

    setHeight(scale * bounds.size[1] + 50)
  }, [nodeSize, canvas, data, height, width, root])

  useEffect(() => {
    data &&
      setExpanded(
        [data.organization.uuid].concat(
          data.organization.descendantOrgs.map(org => org.uuid)
        )
      )
  }, [data])

  useLayoutEffect(() => {
    if (!(link && node && data?.organization && tree.current && root)) {
      return
    }

    const linkSelect = link.selectAll("path").data(tree.current(root).links())

    linkSelect.attr(
      "d",
      d3
        .linkVertical()
        .target(d => ({ x: d.target.x, y: d.target.y - 105 }))
        .x(d => d.x)
        .y(d => d.y)
    )

    linkSelect
      .enter()
      .append("path")
      .attr("class", "link")
      .attr("stroke-opacity", " 0.8")
      .attr(
        "d",
        d3
          .linkVertical()
          .x(d => d.x)
          .y(d => d.y)
      )

    linkSelect.exit().remove()

    const nodeSelect = node
      .selectAll("g.org")
      .data(root.descendants(), d => d.data.uuid)

    nodeSelect
      .transition()
      .duration(transitionDuration)
      .attr("transform", d => `translate(${d.x},${d.y})`)

    const nodeEnter = nodeSelect
      .enter()
      .append("g")
      .attr("class", "org")
      .attr("transform", d => `translate(${d.x},${d.y})`)

    nodeSelect.exit().remove()

    const iconNodeG = nodeEnter.append("g").attr("class", "orgDetails")

    iconNodeG
      .filter(d => d.data.childrenOrgs.length > 0)
      .append("image")
      .attr("class", "orgChildIcon")
      .attr("width", 12)
      .attr("height", 12)
      .attr("x", -6)
      .attr("y", -6)
      .on("click", d => setExpanded(expanded => _xor(expanded, [d.data.uuid])))

    node
      .selectAll("image.orgChildIcon")
      .attr("href", d =>
        expanded.includes(d.data.uuid) ? COLLAPSE_ICON : EXPAND_ICON
      )

    iconNodeG
      .append("g")
      .attr("transform", d => {
        const positions = sortPositions(d.data.positions)
        const unitcode = Settings.fields.person.ranks.find(
          element => element.value === positions?.[0]?.person?.rank
        )?.app6Modifier
        return `translate(${-app6IconSize / 2 - 9},${unitcode ? -86 : -75})`
      })
      .on("click", d => history.push(Organization.pathFor(d.data)))
      .each(function(d) {
        const positions = sortPositions(d.data.positions)
        const unitcode = Settings.fields.person.ranks.find(
          element => element.value === positions?.[0]?.person?.rank
        )?.app6Modifier

        const sym = new Symbol(
          `S${
            d.data.type === Organization.TYPE.ADVISOR_ORG ? "F" : "N"
          }GPU------${unitcode || "-"}`,
          { size: app6IconSize }
        )
        this.appendChild(sym.asDOM())
      })

    iconNodeG
      .append("text")
      .on("click", d => history.push(Organization.pathFor(d.data)))
      .style("text-anchor", "middle")
      .attr("font-size", "17px")
      .attr("font-family", "monospace")
      .attr("font-weight", "bold")
      .attr("dy", -90)
      .text(d =>
        d.data.shortName?.length > 12
          ? d.data.shortName.substring(0, 10) + ".."
          : d.data.shortName
      )

    const headG = nodeSelect.selectAll("g.head").data(
      d => sortPositions(d.data.positions, Math.min(1, personnelDepth)) || [],
      d => d.uuid
    )

    const headGenter = headG
      .enter()
      .append("g")
      .attr("class", "head")
      .attr("transform", "translate(0, -25)")
      .on("click", d => history.push(Position.pathFor(d)))

    headG.exit().remove()

    headGenter
      .append("image")
      .attr("width", avatarSize)
      .attr("height", avatarSize)
      .attr("x", -avatarSize / 2)
      .attr("y", -42)
      .attr(
        "href",
        d =>
          d.person &&
          (d.person.avatar
            ? "data:image/jpeg;base64," + d.person.avatar
            : DEFAULT_AVATAR)
      )

    headGenter
      .append("text")
      .attr("y", -1)
      .attr("font-size", "11px")
      .attr("font-family", "monospace")
      .attr("font-weight", "bold")
      .style("text-anchor", "middle")
      .text((position, i) => {
        const name = `${position.person ? position.person.rank : ""} ${
          position.person
            ? Person.parseFullName(position.person.name).lastName
            : "unfilled"
        }`
        return name.length > 23 ? name.substring(0, 21) + ".." : name
      })

    const positionsG = nodeSelect.selectAll("g.position").data(
      d => sortPositions(d.data.positions, personnelDepth).slice(1),
      d => d.uuid
    )

    positionsG.exit().remove()

    const positionGfn = (d, i, nodes) =>
    `translate(${-6 + ((i - nodes.length / 2) * avatarSize) / 2.4},-12)`

    positionsG
      .enter()
      .append("g")
      .attr("class", "position")
      .attr("transform", positionGfn)
      .on("click", d => history.push(Position.pathFor(d)))
      .append("image")
      .attr("width", avatarSize / 2)
      .attr("height", avatarSize / 2)
      .attr("y", -10)
      .attr("href", d =>
        d.person
          ? d.person.avatar
            ? "data:image/jpeg;base64," + d.person.avatar
            : DEFAULT_AVATAR
          : EMPTY_SET
      )

    positionsG.attr("transform", positionGfn)
  }, [data, expanded, history, personnelDepth, root, link, node])

  if (done) {
    return result
  }

  return (
    <SVGCanvas
      ref={svgRef}
      width={width}
      height={height}
      exportTitle={`${data.shortName} organization chart`}
      zoomFn={increment =>
        setPersonnelDepth(Math.max(0, personnelDepth + increment))}
    >
      <g ref={canvasRef}>
        <g ref={linkRef} style={{ fill: "none", stroke: "#555" }} />
        <g ref={nodeRef} style={{ cursor: "pointer", pointerEvents: "all" }} />
      </g>
    </SVGCanvas>
  )
}

OrganizationalChart.propTypes = {
  pageDispatchers: PageDispatchersPropType,
  org: PropTypes.object.isRequired,
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired
}

export default connect(null, mapPageDispatchersToProps)(OrganizationalChart)
