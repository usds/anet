import PropTypes from 'prop-types'
import React, { Component } from 'react'

export default class Fieldset extends Component {
	static propTypes = {
		title: PropTypes.node,
		action: PropTypes.node,
		stickyHeader: PropTypes.boolean
	}

	render() {
		let {id, title, action, stickyHeader, ...props} = this.props
		const stickyClassName = stickyHeader ? 'sticky-top-2' : ''
		return <div id={id} data-jumptarget={id}>
			<h2 className={`legend ${stickyClassName}`}>
				<span className="title-text">{title}</span>
				{action && <small>{action}</small>}
			</h2>

			<fieldset {...props} />
		</div>
	}
}