import { setPagination } from "actions"
import API from "api"
import { gql } from "apollo-boost"
import ReportCollection, {
  GQL_REPORT_FIELDS,
  GQL_BASIC_REPORT_FIELDS
} from "components/ReportCollection"
import _isEqualWith from "lodash/isEqualWith"
import PropTypes from "prop-types"
import React, { Component } from "react"
import { connect } from "react-redux"
import utils from "utils"

export const FORMAT_CALENDAR = "calendar"
export const FORMAT_SUMMARY = "summary"
export const FORMAT_TABLE = "table"
export const FORMAT_MAP = "map"

const GQL_GET_REPORT_LIST = gql`
  query($reportsQueryParams: ReportSearchQueryInput, $includeAll: Boolean!) {
    reportList(query: $reportsQueryParams) {
      pageNum
      pageSize
      totalCount
      list @include(if: $includeAll) {
        ${GQL_BASIC_REPORT_FIELDS}
      }
      list @skip(if: $includeAll) {
        ${GQL_REPORT_FIELDS}
      }
    }
  }
`

class ReportCollectionContainer extends Component {
  static propTypes = {
    queryParams: PropTypes.object,
    mapId: PropTypes.string,
    pagination: PropTypes.object,
    paginationKey: PropTypes.string,
    setPagination: PropTypes.func.isRequired
  }

  state = {
    curPageReports: null,
    allReports: null
  }

  get curPageNum() {
    const { pagination, paginationKey } = this.props
    return pagination[paginationKey] === undefined
      ? 0
      : pagination[paginationKey].pageNum
  }

  componentDidMount() {
    this.fetchReportData(true)
  }

  componentDidUpdate(prevProps, prevState) {
    // Re-load all data if queryParams has changed
    if (
      !_isEqualWith(
        this.props.queryParams,
        prevProps.queryParams,
        utils.treatFunctionsAsEqual
      )
    ) {
      this.fetchReportData(true)
    }
  }

  reportsQueryParams = (withPagination, pageNum) => {
    const reportsQueryParams = {}
    Object.assign(reportsQueryParams, this.props.queryParams)
    Object.assign(reportsQueryParams, {
      pageNum: withPagination
        ? pageNum === undefined
          ? this.curPageNum
          : pageNum
        : 0,
      pageSize: withPagination ? 10 : 0
    })
    return reportsQueryParams
  }

  getReportsQuery = (reportsQueryParams, includeAll) => {
    return API.query(GQL_GET_REPORT_LIST, { reportsQueryParams, includeAll })
  }

  fetchReportData(includeAll, pageNum) {
    // Query used by the paginated views
    const queries = [
      this.getReportsQuery(this.reportsQueryParams(true, pageNum), false)
    ]
    if (includeAll) {
      // Query used by the map and calendar views
      queries.push(
        this.getReportsQuery(this.reportsQueryParams(false), includeAll)
      )
    }
    return Promise.all(queries).then(values => {
      const stateUpdate = {
        curPageReports: values[0].reportList
      }
      if (includeAll) {
        Object.assign(stateUpdate, {
          allReports: values[1].reportList.list
        })
      }
      this.setState(stateUpdate, () =>
        this.props.setPagination(
          this.props.paginationKey,
          this.state.curPageReports.pageNum
        )
      )
    })
  }

  render() {
    const { curPageReports, allReports } = this.state
    const { queryParams, ...othersProps } = this.props
    return (
      (curPageReports !== null || allReports !== null) && (
        <ReportCollection
          paginatedReports={curPageReports}
          reports={allReports}
          goToPage={this.goToReportsPage}
          {...othersProps}
        />
      )
    )
  }

  goToReportsPage = newPageNum => {
    this.fetchReportData(false, newPageNum)
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  setPagination: (paginationKey, pageNum) =>
    dispatch(setPagination(paginationKey, pageNum))
})

const mapStateToProps = (state, ownProps) => ({
  pagination: state.pagination
})

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ReportCollectionContainer)
