import { DEFAULT_SEARCH_PROPS, PAGE_PROPS_MIN_HEAD } from "actions"
import {
  PageDispatchersPropType,
  mapPageDispatchersToProps,
  useBoilerplate
} from "components/Page"
import React from "react"
import { Button } from "react-bootstrap"
import { connect } from "react-redux"
import { useHistory } from "react-router-dom"

const OnboardingShow = ({ pageDispatchers }) => {
  useBoilerplate({
    pageProps: PAGE_PROPS_MIN_HEAD,
    searchProps: DEFAULT_SEARCH_PROPS,
    pageDispatchers
  })
  const history = useHistory()

  return (
    <div className="onboarding-new">
      <h1>Welcome to ANET</h1>
      <p>
        ANET is a training system for reporting TAA engagements, and learning
        about past engagements and people.
      </p>
      <p>
        Let's create a new account for you. We'll grab your basic information
        and help your super user get you set up.
      </p>
      <div className="create-account-button-wrapper">
        <Button bsStyle="primary" onClick={onCreateAccountClick}>
          Create your account
        </Button>
      </div>
    </div>
  )

  function onCreateAccountClick() {
    history.push("/onboarding/edit")
  }
}

OnboardingShow.propTypes = { pageDispatchers: PageDispatchersPropType }

export default connect(null, mapPageDispatchersToProps)(OnboardingShow)
