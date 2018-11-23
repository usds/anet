import React, {Component} from 'react'

import NoPositionBanner from 'components/NoPositionBanner'
import GeneralBanner from 'components/GeneralBanner'
import SecurityBanner from 'components/SecurityBanner'
import Header from 'components/Header'
import Person from 'models/Person'
import AppContext from 'components/AppContext'

const GENERAL_BANNER_LEVEL = 'GENERAL_BANNER_LEVEL'
const GENERAL_BANNER_TEXT = 'GENERAL_BANNER_TEXT'
const GENERAL_BANNER_VISIBILITY = 'GENERAL_BANNER_VISIBILITY'
const GENERAL_BANNER_TITLE = 'Announcement'
const visible = {
    USERS: 1,
    SUPER_USERS: 2,
    USERS_AND_SUPER_USERS: 3
}

interface BaseTopBarProps {
    currentUser: Person,
    appSettings: object,
    topbarHeight: Function,
    location: String,
    toggleMenuAction: Function,
    minimalHeader: Boolean
}

interface BaseTopBarState {
    bannerVisibility: Boolean,
    height: Number
}

class BaseTopBar extends Component<BaseTopBarProps,BaseTopBarState> {

    protected topbarDiv: any = React.createRef()

    public state = {
        bannerVisibility: false,
        height: 0,
    }

    componentDidMount() {
        this.handleTopbarHeight()
        this.updateBannerVisibility()
        window.addEventListener("resize", this.handleTopbarHeight)
    }

    componentDidUpdate() {
        this.handleTopbarHeight()
        this.updateBannerVisibility()
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.handleTopbarHeight)
    }

    handleTopbarHeight = () => {
        const height = this.topbarDiv.current.clientHeight
        if(height !== undefined && height !== this.state.height) {
            this.setState({ height }, () => this.props.topbarHeight(this.state.height))
        }
    }

    updateBannerVisibility(){
        let visibilitySetting = parseInt(this.props.appSettings[GENERAL_BANNER_VISIBILITY], 10)
        let output = false
        const { currentUser } = this.props
        if (visibilitySetting === visible.USERS && currentUser && !currentUser.isSuperUser()) {
            output = true
        }
        if (visibilitySetting === visible.SUPER_USERS && currentUser && currentUser.isSuperUser()) {
            output = true
        }
        if (visibilitySetting === visible.USERS_AND_SUPER_USERS && (currentUser || currentUser.isSuperUser())) {
            output = true
        }
        if (this.state.bannerVisibility !== output) {
            this.setState({ bannerVisibility: output})
        }
    }

    bannerOptions(){
        return {
            level: this.props.appSettings[GENERAL_BANNER_LEVEL],
            message: this.props.appSettings[GENERAL_BANNER_TEXT],
            title: GENERAL_BANNER_TITLE,
            visible: this.state.bannerVisibility
        } || {}
    }

    render() {
        return (
            <div
                style={{ flex:'0 0 auto', zIndex: 100}}
                ref={this.topbarDiv}
            >
                <div>
                    {this.props.currentUser && !this.props.currentUser.hasAssignedPosition() && !this.props.currentUser.isNewUser() && <NoPositionBanner />}
                    <GeneralBanner options={this.bannerOptions()} />
                    <SecurityBanner location={this.props.location} />
                    <Header minimalHeader={this.props.minimalHeader} toggleMenuAction={this.props.toggleMenuAction}/>
                </div>
            </div>
        )
    }
}

const TopBar = (props) => (
	<AppContext.Consumer>
		{context =>
			<BaseTopBar appSettings={context.appSettings} currentUser={context.currentUser} {...props} />
		}
	</AppContext.Consumer>
)

export default TopBar
