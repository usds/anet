import PropTypes from 'prop-types'
import React, { Component } from 'react'
import {Element} from 'react-scroll'
import Sticky from 'react-stickynode'
export default class Fieldset extends Component {
	static propTypes = {
		title: PropTypes.node,
		action: PropTypes.node,
		stickyAfter: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.number
		])
	}

	static defaultProps = {
		stickyAfter: 200
	}

	render() {
		let {id, title, action, stickyAfter, ...props} = this.props
		return <Element name={id} className="scroll-anchor-container">
			<Sticky enabled={true} top={stickyAfter}>
				<h2 className="legend">
					<span className="title-text">{title}</span>
					{action && <small>{action}</small>}
				</h2>
			</Sticky>
			<fieldset {...props} />
		</Element>
	}
}
