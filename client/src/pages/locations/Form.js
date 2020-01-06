import API from "api"
import { gql } from "apollo-boost"
import AppContext from "components/AppContext"
import Fieldset from "components/Fieldset"
import Leaflet from "components/Leaflet"
import Messages from "components/Messages"
import NavigationWarning from "components/NavigationWarning"
import { jumpToTop } from "components/Page"
import _escape from "lodash/escape"
import { Location, Person } from "models"
import PropTypes from "prop-types"
import React, { useState } from "react"
import {
  Button,
  Form,
  FormGroup,
  FormText,
  FormFeedback,
  Label,
  Input,
  Option,
  Select
} from "@bootstrap-styled/v4"
import { useHistory } from "react-router-dom"
import useForm from "react-hook-form"
import { RHFInput as Field } from "react-hook-form-input"
import { Coordinate } from "./Show"

const GQL_CREATE_LOCATION = gql`
  mutation($location: LocationInput!) {
    createLocation(location: $location) {
      uuid
    }
  }
`
const GQL_UPDATE_LOCATION = gql`
  mutation($location: LocationInput!) {
    updateLocation(location: $location)
  }
`

// const CustomInput = ({ label, onChange, register, required }) =>
//   <>
//     <label>{label}</label>
//     <Input name={label} ref={register({ type: "custom" })} onChange={onChange} />
//   </>

// function InputField({ register, setValue, name }) {
//   // const onValueChange = useCallback(
//   //   e => ({
//   //     value: e[0].floatValue
//   //   }),
//   //   []
//   // );

//   return (
//     <RHFInput
//       register={register}
//       mode="onChange"
//       name={name}
//       setValue={setValue}
//       as={<Input />}
//     />
//   )
// }

const ErrorMessage = ({ errors, name }) => {
  // Note: if you are using FormContext, then you can use Errors without props eg:
  // const { errors } = useFormContext();
  if (!errors[name]) return null

  return errors[name] && <p><FormFeedback color="muted">{errors[name].message}</FormFeedback></p>
}

const statusButtons = [
  {
    id: "statusActiveButton",
    value: Location.STATUS.ACTIVE,
    label: "Active"
  },
  {
    id: "statusInactiveButton",
    value: Location.STATUS.INACTIVE,
    label: "Inactive"
  }
]

const marker = ({ location, handleMarkerMove }) => {
  return ({
    id: location.id || 0,
    name: _escape(location.name) || "", // escape HTML in location name!
    lat: location.lat,
    lng: location.lng,
    draggable: true,
    autoPan: true,
    onMove: (event, map) => handleMarkerMove(event, map)
  })
}

const LocationField = ({ register, setValue, marker }) =>
  <FormGroup>
    <Field
      name="lat"
      as={<Input type="hidden" />}
      register={register}
      setValue={setValue}
      type="hidden"
    />
    <Field
      name="lng"
      as={<Input type="hidden" />}
      register={register}
      setValue={setValue}
    />
    <Leaflet
      markers={[marker]}
    />
  </FormGroup>

const SubmitButtonRow = ({ children }) =>
  <div className="submit-buttons">
    <div>{children[0]}</div>
    {children[1]}
  </div>

const BaseLocationForm = props => {
  const { currentUser, edit, title, initialValues } = props // currentUser, edit, ...myFormProps
  // const { handleSubmit } = useForm() // , register, errors
  const history = useHistory()
  const [error, setError] = useState(null)
  const canEditName =
    (!edit && currentUser.isSuperUser()) || (edit && currentUser.isAdmin())

  const { register, handleSubmit, formState, setValue, watch, reset, errors } = useForm({
    defaultValues: { ...initialValues },
    validationSchema: Location.yupSchema,
    mode: "onChange"
  })
  const location = watch(["uuid", "name", "lat", "lng"])
  const { uuid, lat, lng } = location

  const handleMarkerMove = (event, map) => {
    const latLng = map.wrapLatLng(event.latlng)
    setValue("lat", latLng.lat)
    setValue("lng", latLng.lng)
  }

  const action = (
    <div>
      <Button
        key="submit"
        type="submit"
        onClick={handleSubmit(onSubmit)}
        disabled={formState.isSubmitting}
      >
        Save Location
      </Button>
    </div>
  )

  return (
    <div className="LocationForm">
      <NavigationWarning isBlocking={formState.dirty} />
      <Messages error={error} />
      <Form onSubmit={handleSubmit(onSubmit)}>
        <Fieldset title={title} action={action} />
        <Fieldset>
          <FormGroup color={errors["name"] && "danger"}>
            <Label htmlFor="locationname">Name</Label>
            <Field
              register={register}
              mode="onChange"
              name="name"
              id="locationName"
              setValue={setValue}
              as={<Input />}
              disabled={!canEditName}
              state={errors["name"] && "danger"}
            />
            <ErrorMessage {...{ errors, name: "name" }} />
          </FormGroup>
          <FormGroup>
            <Label htmlFor="locationStatus">Status</Label>
            <Field
              id="locationStatus"
              name="status"
              as={<Select />}
              children={statusButtons.map(item => (
                <Option key={item.id} value={item.value}>
                  {item.label}
                </Option>
              ))}
              register={register}
              setValue={setValue}
            />
            <ErrorMessage {...{ errors, name: "status" }} />
          </FormGroup>
          <Label>Location</Label>
          <FormText>
            <Coordinate coord={lat || 0} />, <Coordinate coord={lng || 0} />
          </FormText>
        </Fieldset>

        <Fieldset title="Drag the marker below to set the location">
          <LocationField
            marker={marker({ ...{ location, handleMarkerMove } })}
            register={register}
            setValue={setValue}
          />
        </Fieldset>
        <SubmitButtonRow>
          <Button onClick={onCancel}>Cancel</Button>
          {action}
        </SubmitButtonRow>
      </Form>
    </div>
  )

  function onCancel() {
    history.goBack()
  }

  function onSubmit(values) {
    return save(values)
      .then(response => onSubmitSuccess(response, values))
      .catch(error => {
        setError(error)
        jumpToTop()
      })
  }

  function onSubmitSuccess(response) {
    const { edit } = props
    const operation = edit ? "updateLocation" : "createLocation"
    const location = new Location({
      uuid: response[operation].uuid
        ? response[operation].uuid
        : props.initialValues.uuid
    })
    // After successful submit, reset the form in order to make sure the dirty
    // prop is also reset (otherwise we would get a blocking navigation warning)
    reset()
    if (!edit) {
      history.replace(Location.pathForEdit(location))
    }
    history.push(Location.pathFor(location), {
      success: "Location saved"
    })
  }

  function save(values) {
    const location = Object.assign(values, { uuid })
    return API.mutation(
      props.edit ? GQL_UPDATE_LOCATION : GQL_CREATE_LOCATION,
      { location }
    )
  }
}

BaseLocationForm.propTypes = {
  initialValues: PropTypes.instanceOf(Location).isRequired,
  title: PropTypes.string,
  edit: PropTypes.bool,
  currentUser: PropTypes.instanceOf(Person)
}

BaseLocationForm.defaultProps = {
  initialValues: new Location(),
  title: "",
  edit: false
}

const LocationForm = props => (
  <AppContext.Consumer>
    {context => (
      <BaseLocationForm currentUser={context.currentUser} {...props} />
    )}
  </AppContext.Consumer>
)

export default LocationForm
