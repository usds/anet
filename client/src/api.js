import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({
	parent: '.header'
})

const API = {
	fetch(url, params) {
		params = params || {}
		params.credentials = 'same-origin'

		params.headers = params.headers || {}
		params.headers['Accept'] = 'application/json'

		if (params.showLoader !== false)
			API.startLoading()
		delete params.showLoader

		return window.fetch(url, params)
					.then(response => {
						API.stopLoading()

						let isOk = response.ok

						if (response.headers.get('content-type') === 'application/json') {
							response = response.json()
							if (!isOk)
								return response.then(response => Promise.reject(response))
						}

						if (!isOk)
							response = Promise.reject(response)

						return response
					})
	},

	send(url, data, params) {
		params = params || {}
		params.disableSubmits = typeof params.disableSubmits === 'undefined' ? true : params.disableSubmits
		params.method = params.method || 'POST'
		params.body = JSON.stringify(data)
		
		params.headers = params.headers || {}
		params.headers['Content-Type'] = 'application/json'

		let promise = API.fetch(url, params)
		let buttons = document.querySelectorAll('[type=submit]')
		let toggleButtons =  function(onOff) {
			for (let button of buttons) {
				button.disabled = !onOff
			}
		}

		if (params.disableSubmits) {
			toggleButtons(false)

			promise.then(response => {
				toggleButtons(true)
				return response
			}, response => {
				toggleButtons(true)
				return response
			})
		}

		return promise
	},

	query(query, variables) {
		query = 'query { ' + query + ' }'
		variables = variables || {}
		return API.send('/graphql', {query, variables}).then(json => json.data)
	},

	startLoading() {
		NProgress.start()
		NProgress.set(0.5)
	},

	stopLoading() {
		NProgress.done()
	},
}

export default API
