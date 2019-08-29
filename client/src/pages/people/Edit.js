import { PAGE_PROPS_NO_NAV } from "actions"
import API from "api"
import { gql } from "apollo-boost"
import {
  mapDispatchToProps,
  propTypes as pagePropTypes,
  useBoilerplate
} from "components/Page"
import RelatedObjectNotes, {
  GRAPHQL_NOTES_FIELDS
} from "components/RelatedObjectNotes"
import { Person } from "models"
import moment from "moment"
import React from "react"
import { connect } from "react-redux"
import PersonForm from "./Form"

const GQL_GET_PERSON = gql`
  query($uuid: String!) {
    person(uuid: $uuid) {
      uuid
      name
      rank
      role
      emailAddress
      phoneNumber
      status
      domainUsername
      biography
      country
      gender
      endOfTourDate
      avatar(size: 256)
      position {
        uuid
        name
        type
      }
      ${GRAPHQL_NOTES_FIELDS}
    }
  }
`

const PersonEdit = props => {
  const uuid = props.match.params.uuid
  const { loading, error, data } = API.useApiQuery(GQL_GET_PERSON, {
    uuid
  })
  const { done, result } = useBoilerplate({
    loading,
    error,
    modelName: "User",
    uuid,
    pageProps: PAGE_PROPS_NO_NAV,
    ...props
  })
  if (done) {
    return result
  }

  if (data) {
    if (data.person.endOfTourDate) {
      data.person.endOfTourDate = moment(data.person.endOfTourDate).format()
    }
    const parsedFullName = Person.parseFullName(data.person.name)
    data.person.firstName = parsedFullName.firstName
    data.person.lastName = parsedFullName.lastName
  }
  const person = new Person(data ? data.person : {})
  const legendText = person.isNewUser()
    ? "Create your account"
    : `Edit ${person.name}`
  const saveText = person.isNewUser() ? "Create profile" : "Save Person"

  return (
    <div>
      <RelatedObjectNotes
        notes={person.notes}
        relatedObject={
          person.uuid && {
            relatedObjectType: "people",
            relatedObjectUuid: person.uuid
          }
        }
      />
      <PersonForm
        initialValues={person}
        edit
        title={legendText}
        saveText={saveText}
      />
    </div>
  )
}

PersonEdit.propTypes = {
  ...pagePropTypes
}

export default connect(
  null,
  mapDispatchToProps
)(PersonEdit)
