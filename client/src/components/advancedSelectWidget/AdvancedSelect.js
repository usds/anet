import { Popover, PopoverInteractionKind, Position } from "@blueprintjs/core"
import API from "api"
import { gql } from "apollo-boost"
import * as FieldHelper from "components/FieldHelper"
import UltimatePagination from "components/UltimatePagination"
import _debounce from "lodash/debounce"
import _isEmpty from "lodash/isEmpty"
import _isEqual from "lodash/isEqual"
import PropTypes from "prop-types"
import React, { Component } from "react"
import { Button, Col, FormControl, InputGroup, Row } from "react-bootstrap"
import "./AdvancedSelect.css"

const hasMultipleItems = object => Object.keys(object).length > 1

const AdvancedSelectTarget = ({ overlayRef }) => (
  <Row>
    <Col
      className="form-group"
      ref={overlayRef}
      style={{ position: "relative", marginBottom: 0 }}
    />
  </Row>
)
AdvancedSelectTarget.propTypes = {
  overlayRef: PropTypes.shape({
    current: PropTypes.oneOfType([PropTypes.func, PropTypes.object])
  })
}

const FilterAsNav = ({ items, currentFilter, handleOnClick }) =>
  hasMultipleItems(items) && (
    <Col md={2} xsHidden smHidden>
      <ul className="advanced-select-filters" style={{ paddingInlineStart: 0 }}>
        {Object.keys(items).map(filterType => (
          <li
            key={filterType}
            className={currentFilter === filterType ? "active" : null}
          >
            <Button bsStyle="link" onClick={() => handleOnClick(filterType)}>
              {items[filterType].label}
            </Button>
          </li>
        ))}
      </ul>
    </Col>
  )
FilterAsNav.propTypes = {
  items: PropTypes.object,
  currentFilter: PropTypes.string,
  handleOnClick: PropTypes.func
}

const FilterAsDropdown = ({ items, handleOnChange }) =>
  hasMultipleItems(items) && (
    <Col style={{ minHeight: "80px" }} mdHidden lgHidden>
      <p style={{ padding: "5px 0" }}>
        Filter:
        <select onChange={handleOnChange} style={{ marginLeft: "5px" }}>
          {Object.keys(items).map(filterType => (
            <option key={filterType} value={filterType}>
              {items[filterType].label}
            </option>
          ))}
        </select>
      </p>
    </Col>
  )
FilterAsDropdown.propTypes = {
  items: PropTypes.object,
  handleOnChange: PropTypes.func
}

export const propTypes = {
  fieldName: PropTypes.string.isRequired, // input field name
  placeholder: PropTypes.string, // input field placeholder
  disabled: PropTypes.bool,
  searchTerms: PropTypes.string,
  addon: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func,
    PropTypes.object
  ]),
  extraAddon: PropTypes.object,
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  renderSelected: PropTypes.oneOfType([PropTypes.func, PropTypes.object]), // how to render the selected items
  overlayTableClassName: PropTypes.string,
  overlayTable: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.func,
    PropTypes.object
  ]), // search results component for in the overlay
  overlayColumns: PropTypes.array.isRequired,
  overlayRenderRow: PropTypes.func.isRequired,
  closeOverlayOnAdd: PropTypes.bool, // set to true if you want the overlay to be closed after an add action
  filterDefs: PropTypes.object, // config of the search filters
  onChange: PropTypes.func,
  // Required: ANET Object Type (Person, Report, etc) to search for.
  objectType: PropTypes.func.isRequired,
  // Optional: Parameters to pass to all search filters.
  queryParams: PropTypes.object,
  // Optional: GraphQL string of fields to return from search.
  fields: PropTypes.string,
  handleAddItem: PropTypes.func,
  handleRemoveItem: PropTypes.func
}

export default class AdvancedSelect extends Component {
  static defaultProps = {
    disabled: false,
    filterDefs: {},
    closeOverlayOnAdd: false,
    searchTerms: ""
  }

  state = {
    searchTerms: this.props.searchTerms,
    filterType: Object.keys(this.props.filterDefs)[0], // per default use the first filter
    results: {},
    showOverlay: false,
    isLoading: false
  }

  overlayContainer = React.createRef()
  overlayTarget = React.createRef()

  componentDidMount() {
    this.setState({
      searchTerms: this.props.searchTerms || ""
    })
  }

  componentDidUpdate(prevProps, prevState) {
    if (!_isEqual(prevProps.searchTerms, this.props.searchTerms)) {
      this.setState({ searchTerms: this.props.searchTerms })
    }
    if (
      !_isEqual(prevState.showOverlay, this.state.showOverlay) &&
      this.state.showOverlay === false &&
      !_isEqual(this.props.searchTerms, this.state.searchTerms)
    ) {
      // When the overlay is being closed, update the searchTerms with the selected value
      this.setState({ searchTerms: this.props.searchTerms || "" })
    }
  }

  render() {
    const {
      closeOverlayOnAdd,
      fieldName,
      placeholder,
      disabled,
      value,
      renderSelected,
      onChange,
      objectType,
      queryParams,
      fields,
      handleAddItem,
      handleRemoveItem,
      addon,
      extraAddon,
      ...overlayProps
    } = this.props

    const {
      overlayTableClassName,
      overlayColumns,
      overlayRenderRow,
      filterDefs
    } = overlayProps

    const {
      results,
      filterType,
      isLoading,
      searchTerms,
      showOverlay
    } = this.state

    const renderSelectedWithDelete = renderSelected
      ? React.cloneElement(renderSelected, { onDelete: handleRemoveItem })
      : null
    const items = results && results[filterType] ? results[filterType].list : []
    const pageNum =
      results && results[filterType] ? results[filterType].pageNum : 0

    const advancedSearchPopoverContent = (
      <Row className="border-between">
        <FilterAsNav
          items={filterDefs}
          currentFilter={filterType}
          handleOnClick={this.changeFilterType}
        />

        <FilterAsDropdown
          items={filterDefs}
          handleOnChange={this.handleOnChangeSelect}
        />

        <Col md={hasMultipleItems(filterDefs) ? 10 : 12}>
          <this.props.overlayTable
            fieldName={fieldName}
            items={items}
            pageNum={pageNum}
            selectedItems={value}
            handleAddItem={item => {
              handleAddItem(item)
              if (closeOverlayOnAdd) {
                this.handleHideOverlay()
              }
            }}
            handleRemoveItem={handleRemoveItem}
            objectType={objectType}
            columns={[""].concat(overlayColumns)}
            renderRow={overlayRenderRow}
            isLoading={isLoading}
            loaderMessage={
              <div style={{ width: "300px" }}>No results found</div>
            }
            tableClassName={overlayTableClassName}
          />
          {this.paginationFor(filterType)}
        </Col>
      </Row>
    )

    return (
      <>
        {!(disabled && renderSelectedWithDelete) && (
          <>
            <div id={`${fieldName}-popover`}>
              <InputGroup>
                <Popover
                  className="advanced-select-popover"
                  popoverClassName="bp3-popover-content-sizing"
                  content={advancedSearchPopoverContent}
                  isOpen={showOverlay}
                  captureDismiss
                  disabled={disabled}
                  interactionKind={PopoverInteractionKind.CLICK}
                  onInteraction={this.handleInteraction}
                  usePortal={false}
                  position={Position.BOTTOM}
                  modifiers={{
                    preventOverflow: {
                      enabled: false
                    },
                    hide: {
                      enabled: false
                    },
                    flip: {
                      enabled: false
                    }
                  }}
                >
                  <FormControl
                    name={fieldName}
                    value={searchTerms || ""}
                    placeholder={placeholder}
                    onChange={this.changeSearchTerms}
                    onFocus={disabled ? undefined : this.handleInputFocus}
                    inputRef={ref => {
                      this.searchInput = ref
                    }}
                    disabled={disabled}
                  />
                </Popover>
                {extraAddon && (
                  <InputGroup.Addon>{extraAddon}</InputGroup.Addon>
                )}
                {addon && (
                  <FieldHelper.FieldAddon fieldId={fieldName} addon={addon} />
                )}
              </InputGroup>
            </div>
            <AdvancedSelectTarget overlayRef={this.overlayContainer} />
          </>
        )}
        <Row>
          <Col sm={12}>{renderSelectedWithDelete}</Col>
        </Row>
      </>
    )
  }

  handleInputFocus = () => {
    if (this.state.showOverlay) {
      return // Overlay is already open and we do not need to fetch data
    }
    this.setState(
      {
        showOverlay: true,
        searchTerms: "",
        isLoading: true
      },
      this.fetchResults()
    )
  }

  handleInteraction = (showOverlay, event) => {
    const inputFocus = this.searchInput.contains(event && event.target)
    return this.setState({ showOverlay: showOverlay || inputFocus })
  }

  handleHideOverlay = () => {
    this.setState({
      filterType: Object.keys(this.props.filterDefs)[0],
      searchTerms: "",
      results: {},
      isLoading: false,
      showOverlay: false
    })
  }

  changeSearchTerms = event => {
    // Reset the results state when the search terms change
    this.setState(
      {
        isLoading: true,
        searchTerms: event.target.value,
        results: {}
      },
      () => this.fetchResultsDebounced()
    )
  }

  handleOnChangeSelect = event => {
    this.changeFilterType(event.target.value)
  }

  changeFilterType = filterType => {
    // When changing the filter type, only fetch the results if they were not fetched before
    const { results } = this.state
    const filterResults = results[filterType]
    const doFetchResults = _isEmpty(filterResults)
    this.setState({ filterType, isLoading: doFetchResults }, () => {
      if (doFetchResults) {
        this.fetchResults()
      }
    })
  }

  fetchResults = (pageNum = 0) => {
    const { filterType, results } = this.state
    const filterDefs = this.props.filterDefs[filterType]
    if (filterDefs.list) {
      // No need to fetch the data, it is already provided in the filter definition
      this.setState({
        isLoading: !_isEmpty(filterDefs.list),
        results: {
          ...results,
          [filterType]: {
            list: filterDefs.list,
            pageNum: pageNum,
            pageSize: 6,
            totalCount: filterDefs.list.length
          }
        }
      })
    } else {
      // GraphQL search type of query
      this.queryResults(filterDefs, filterType, results, pageNum)
    }
  }

  queryResults = (filterDefs, filterType, oldResults, pageNum) => {
    const resourceName = this.props.objectType.resourceName
    const listName = filterDefs.listName || this.props.objectType.listName
    this.setState({ isLoading: true }, () => {
      const queryVars = { pageNum: pageNum, pageSize: 6 }
      if (this.props.queryParams) {
        Object.assign(queryVars, this.props.queryParams)
      }
      if (filterDefs.queryVars) {
        Object.assign(queryVars, filterDefs.queryVars)
      }
      if (this.state.searchTerms) {
        Object.assign(queryVars, { text: this.state.searchTerms + "*" })
      }
      const thisRequest = (this.latestRequest = API.query(
        gql`
          query($query: ${resourceName}SearchQueryInput) {
            ${listName}(query: $query) {
              pageNum
              pageSize
              totalCount
              list {
                ${this.props.fields}
              }
            }
          }
        `,
        { query: queryVars }
      ).then(data => {
        // If this is true there's a newer request happening, stop everything
        if (thisRequest !== this.latestRequest) {
          return
        }
        const isLoading = data[listName].totalCount !== 0
        this.setState({
          isLoading,
          results: {
            ...oldResults,
            [filterType]: data[listName]
          }
        })
      }))
    })
  }

  fetchResultsDebounced = _debounce(this.fetchResults, 400)

  paginationFor = filterType => {
    const { results } = this.state
    const pageSize =
      results && results[filterType] ? results[filterType].pageSize : 6
    const pageNum =
      results && results[filterType] ? results[filterType].pageNum : 0
    const totalCount =
      results && results[filterType] ? results[filterType].totalCount : 0
    return (
      <UltimatePagination
        Component="footer"
        componentClassName="searchPagination"
        className="pull-right"
        pageNum={pageNum}
        pageSize={pageSize}
        totalCount={totalCount}
        goToPage={this.goToPage}
      />
    )
  }

  goToPage = pageNum => {
    this.fetchResults(pageNum)
  }
}

AdvancedSelect.propTypes = propTypes
