import PropTypes from 'prop-types'
import React, { Component } from 'react'
import {Element} from 'react-scroll'

export default class Fieldset extends Component {
	static propTypes = {
		title: PropTypes.node,
		action: PropTypes.node,
		stickyClass: PropTypes.string
	}

	static defaultProps = {
		stickyClass: "sticky-top-2"
	}

	render() {
		let {id, title, action, stickyClass, ...props} = this.props
		return <Element name={id} className="scroll-anchor-container">
			<h2 className={`legend ${this.props.stickyClass}`}>
				<span className="title-text">{title}</span>
				{action && <small>{action}</small>}
			</h2>

			<fieldset {...props} />
		</Element>
	}
}
