import React from 'react'
import ReactDOM from 'react-dom'
import {Form as BSForm, Button} from 'react-bootstrap'

import {ContentForHeader} from 'components/Header'
import FormField from 'components/FormField'

export default class Form extends React.Component {
	static propTypes = Object.assign({}, BSForm.propTypes, {
		formFor: React.PropTypes.object,
		actionText: React.PropTypes.string,
		onSubmit: React.PropTypes.func,
	})

	static childContextTypes = {
		formFor: React.PropTypes.object,
		form: React.PropTypes.object,
	}

	getChildContext() {
		return {
			formFor: this.props.formFor,
			form: this,
		}
	}

	componentDidMount() {
		let container = ReactDOM.findDOMNode(this.refs.container)
		let focusElement = container.querySelector('[data-focus]')
		if (focusElement) focusElement.focus()
	}

	render() {
		let {children, actionText, ...bsProps} = this.props
		bsProps = Object.without(bsProps, 'formFor')

		let showSubmit = bsProps.onSubmit && actionText !== false

		return (
			<BSForm {...bsProps} ref="container">
				{children}

				{showSubmit &&
					<ContentForHeader>
						<Button bsStyle="primary" type="submit" onClick={bsProps.onSubmit}>
							{actionText || "Save"}
						</Button>
					</ContentForHeader>
				}

				{showSubmit &&
					<fieldset>
						<Button bsStyle="primary" bsSize="large" type="submit" className="pull-right">
							{actionText || "Save"}
						</Button>
					</fieldset>
				}
			</BSForm>
		)
	}
}

// just a little sugar to make importing and building forms easier
Form.Field = FormField
