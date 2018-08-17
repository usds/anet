import React, {Component} from 'react'
import {Breadcrumb} from 'react-bootstrap'
import {IndexLinkContainer as Link} from 'react-router-bootstrap'
import Sticky from 'react-stickynode'

export default class Breadcrumbs extends Component {
	makeItem(item) {
		return (
			<Link key={item[1]} to={item[1]}>
				<Breadcrumb.Item>{item[0]}</Breadcrumb.Item>
			</Link>
		)
	}

	render() {
		let {items, ...props} = this.props

		return (
			<Sticky enabled={true} top={".topbar"}>
				<Breadcrumb {...props}>
					{this.makeItem(['ANET', '/'])}
					{items.map(this.makeItem)}
				</Breadcrumb>
			</Sticky>
		)
	}
}

Breadcrumbs.defaultProps = {items: []}
