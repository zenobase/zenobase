(function() {

	'use strict';

	/**
	 * Stub console object if not present.
	 */
	(function(console) {
		$.each([ 'assert', 'log' ], function(i, method) {
			console[method] = console[method] || function() {};
		});
	}(window.console = window.console || {}));

	/**
	 * Prevent charts from capturing single finger swipes.
	 */
	(function() {
		Highcharts.wrap(Highcharts.Pointer.prototype, 'pinch', function(proceed, e) {
			if (e.touches.length > 1) {
				proceed.call(this, e);
			}
		});
	}());

	var app = angular.module('appModule', [ 'ngRoute', 'ngSanitize' ]);

	app.factory('delay', ['$timeout', function($timeout) {
		return function(callback) {
			$timeout(callback, 1000);
		};
	}]);

	app.factory('localStorage', ['$window', function($window) {
		var store = {};
		return $window.localStorage || {
			getItem : function(key) {
				return store[key];
			},
			setItem : function(key, value) {
				store[key] = value;
			},
			removeItem : function(key) {
				delete store[key];
			}
		};
	}]);

	app.factory('moment', function() {

		// See https://github.com/timrwood/moment/issues/537
		moment.fn.fromNowOrNow = function(alwaysRelative, a) {
			var diff = Math.abs(moment().diff(this));
			if (diff < 60000) { // less than a minute
				return 'just now';
			}
			if (!alwaysRelative && diff >= 79200000) { // 22 hours or more
				return this.format('MMM D, YYYY HH:mm');
			}
			return this.fromNow(a);
		};

		// See https://github.com/timrwood/moment/issues/463
		moment.duration.fn.countdown = function(precision) {
			var args = [];
			if (this.years()) {
				args.push(this.years() + 'y');
			}
			if (this.months()) {
				args.push(this.months() + 'm');
			}
			if (this.days()) {
				args.push(this.days() + 'd');
			}
			if (this.hours()) {
				args.push(this.hours() + 'h');
			}
			if (this.minutes()) {
				args.push(this.minutes() + 'min');
			}
			if (this.seconds()) {
				args.push(this.seconds() + 's');
			}
			if (precision > 0 && args.length > 1) {
				args = args.slice(0, precision);
			}
			return args.length ? args.join(' ') : this.milliseconds() + 'ms';
		};

		moment.duration.fn.countdownCompact = function() {
			var minutes = Math.floor(this.asMinutes());
			var seconds = this.seconds();
			if (seconds < 10) {
				seconds = '0' + seconds;
			}
			return minutes + '\'' + seconds + '"';
		};

		return moment;
	});

	app.factory('token', ['$http', 'localStorage', function($http, localStorage) {
		var key = 'access_token';
		var get = function() {
			return localStorage.getItem(key);
		};
		var set = function(token) {
			if (token) {
				localStorage.setItem(key, token);
			} else {
				localStorage.removeItem(key);
			}
			configure(token);
		};
		var configure = function(token) {
			$http.defaults.headers.common['Authorization'] = token ? 'Bearer ' + token : null;
		};
		configure(get());
		return {
			get : get,
			set : set
		};
	}]);

	app.factory('tracker', [function() {
		var toTitleCase = function(str) {
			return str.replace(/\w\S*/g, function(token) {
				return token.charAt(0).toUpperCase() + token.substring(1).toLowerCase();
			});
		};
		return {
			identify : function(userId) {
				mixpanel.identify(userId);
			},
			event : function(category, action, properties) {
				category = toTitleCase(category);
				action = toTitleCase(action);
				properties = properties || {};
				properties[category] = action;
				mixpanel.track(category, properties);
			},
			pageview : function(page) {
				mixpanel.track_pageview({
					'Page': page
				});
			}
		};
	}]);

	app.factory('timezone', ['moment', function(moment) {
		return moment().format('Z');
	}]);

	app.factory('braintree', ['$window', function($window) {
		return $window.braintree;
	}]);

	app.constant('merchantId', 'tzq6d3tr7npjqzpt');

	// TODO should inject this, but can't inject into config...
	var cacheBuster = function() {
		var version = (function() {
			try {
				throw new Error();
			} catch(e) {
				var callerIndex = 0;
				var stackLines = e.stack.split('\n');
				for (var i in stackLines) {
					if (stackLines[i].match(/http[s]?:\/\//)) {
						callerIndex = Number(i) + 2;
						break;
					}
				}
				var m = stackLines[callerIndex].match(/-([0-9a-f]+).js:/);
				if (!m) {
					throw new Error('missing version');
				}
				return m[1];
			}
		})();
		return {
			rewrite : function(path) {
				return path.replace(/\.(.+)$/, '-' + version + '.$1');
			}
		};
	}();

	app.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/', { templateUrl: cacheBuster.rewrite('/partials/home.html') })
			.when('/buckets/:bucketId/', { templateUrl : cacheBuster.rewrite('/partials/dashboard.html'), reloadOnSearch : false })
			.when('/credentials/:credentialsId', { templateUrl : cacheBuster.rewrite('/partials/credentials.html') })
			.when('/users/:username', { templateUrl : cacheBuster.rewrite('/partials/user.html') })
			.when('/users/:username/reset', { templateUrl : cacheBuster.rewrite('/partials/reset.html') })
			.when('/users/:username/verify', { templateUrl : cacheBuster.rewrite('/partials/verification.html') })
			.when('/oauth/authorize', { templateUrl : cacheBuster.rewrite('/partials/oauth.html') })
			.when('/legal/:section?', { title : 'Legal', templateUrl : cacheBuster.rewrite('/partials/legal.html'), controller : 'DocumentController' })
			.when('/api/:section?', { title : 'API', templateUrl : cacheBuster.rewrite('/partials/api.html'), controller : 'DocumentController' })
			.when('/pricing/', { title : 'Pricing', templateUrl : cacheBuster.rewrite('/partials/pricing.html') })
			.otherwise({ templateUrl : cacheBuster.rewrite('/partials/404.html') });
	}]);

	app.run(['$rootScope', function($rootScope) {
		$rootScope.page = {
			setTitle: function(title) {
				this.title = (title ? title + ' | ' : '') + 'Zenobase';
			}
		};
		$rootScope.$on('$routeChangeSuccess', function(event, current) {
			if (current.$$route) {
				$rootScope.page.setTitle(current.$$route.title);
			}
		});
	}]);

	app.factory('$exceptionHandler', ['$log', 'tracker', function($log, tracker) {
		return function(e) {
			$log.error.apply($log, arguments);
		};
	}]);

	app.controller('ApplicationController', ['$scope', '$route', '$http', '$location', 'Alert', 'User', 'token', 'tracker', 'delay', function($scope, $route, $http, $location, Alert, User, token, tracker, delay) {

		$scope.alert = new Alert();

		$scope.whoami = function(success) {
			$http.get('/who').success(function(response) {
				$scope.user = response ? new User(response) : null;
				if ($scope.user) {
					if (success) {
						success($scope.user);
						tracker.identify($scope.user['@id']);
					}
				}
			});
		};
		$scope.undo = function(commandId) {
			$scope.alert.clear();
			$http.post('/journal/' , { 'undo' : commandId })
				.success(function() {
					delay($route.reload);
				})
				.error(function() {
					$scope.alert.show('Couldn\'t undo.');
				});
			tracker.event('action', 'undo');

		};
		$scope.broadcast = function(event) {
			$scope.$broadcast(event);
		};
		$scope.signOut = function() {
			console.assert(token.get(), 'missing token');
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/authorizations/' + token.get() })
				.success(function() {
					token.set(null);
					$scope.user = null;
					if ($location.url() === '/') {
						$route.reload();
					} else {
						$scope.home();
					}
			});
			tracker.event('action', 'sign out');
		};
		$scope.home = function() {
			$location.url('/');
		};
		$scope.reload = function() {
			$route.reload();
		};
		$scope.openDialog = function(dialog, param) {
			$('input:focus').blur();
			$scope.$broadcast('openDialog', dialog, param);
		};
		$scope.openPage = function(path) {
			$scope.closeDialog();
			$location.url(path);
		};
		$scope.openDialogInPage = function(path, dialog, param) {
			$scope.openPage(path);
			delay(function() {
				$scope.openDialog(dialog, param);
			});
		};
		$scope.closeDialog = function() {
			$scope.openDialog(null);
		};

		$scope.$on('$routeChangeStart', function() {
			$scope.alert.clear();
		});
		$scope.$on('$routeChangeSuccess', function() {
			var path = $location.path().replace(/^\/(users|buckets)\/.+$/, '/$1/'); // remove personally identifiable information
			tracker.pageview(path);
		});
		$scope.whoami();
	}]);

	app.factory('Alert', function() {

		var Alert = function() {
			this.clear();
		};

		Alert.prototype.show = function(message, level, undo, onClick) {
			this.message = message;
			this.level = level;
			this.undo = undo;
			this.onClick = onClick;
		};

		Alert.prototype.clear = function() {
			this.message = '';
			this.level = 'hide';
			this.undo = '';
			this.onClick = null;
		};

		return Alert;
	});

	app.factory('User', [ '$http', '$cacheFactory', function($http, $cacheFactory) {

		var cache = $cacheFactory('User', { capacity : 100 });

		var User = function(data) {
			$.extend(this, data);
			cache.put(this['@id'], this);
		};

		User.prototype.getName = function() {
			return this.name || 'guest';
		};

		User.prototype.isGuest = function() {
			return !this.name;
		};

		User.find = function(id) {
			console.assert(id, "Can't find a user without an id");
			var user = cache.get(id);
			if (!user) {
				$.ajax('/users/' + id, { async : false, success : function(response) {
					user = new User(response);
					cache.put(user['@id'], user);
				}});
			}
			return user;
		};

		return User;
	}]);

	app.factory('Constraint', function() {

		var fieldSeparator = ':';
		var subfieldSeparator = '$';

		var Constraint = function(field, value, negated, subfield) {
			this.field = field;
			this.value = value.toString();
			this.negated = negated;
			this.subfield = subfield;
		};

		Constraint.prototype.invert = function() {
			return new Constraint(this.field, this.value, !this.negated, this.subfield);
		};

		Constraint.prototype.toString = function() {
			var field = this.field + (this.subfield ? subfieldSeparator + this.subfield : '');
			return (this.negated ? '-' : '') + field + fieldSeparator + this.value;
		};

		Constraint.prototype.shortValue = function() {
			var p = this.value.indexOf(' OR ');
			return p === -1 ? this.value : this.value.substring(0, p) + '...';
		};

		Constraint.parse = function(s) {
			var negated = false;
			if (s.length > 1 && s.charAt(0) == '-') {
				negated = true;
				s = s.substring(1);
			}
			var pos = s.indexOf(fieldSeparator);
			if (pos < 1 || pos > s.length - 1) {
				throw 'Can\'t parse constraint: ' + s;
			}
			var field = s.substring(0, pos);
			var subfield = null;
			var pos2 = field.indexOf(subfieldSeparator);
			if (pos2 > 0) {
				subfield = field.substring(pos2 + 1);
				field = field.substring(0, pos2);
			}
			var value = s.substring(pos + 1);
			return new Constraint(field, value, negated, subfield);
		};

		return Constraint;
	});

	app.factory('Spreadsheet', function() {

		var Spreadsheet = function(headers) {
			this.headers = headers;
			this.records = [];
		};

		Spreadsheet.prototype.addHeader = function(header) {
			this.headers.push(header);
		};

		Spreadsheet.prototype.addRecord = function(record) {
			this.records.push(record);
		};

		Spreadsheet.prototype.mergeRecord = function(record) {
			console.assert(record.length === 2);
			for (var i = 0; i < this.records.length; ++i) {
				if (this.records[i][0] === record[0]) {
					this.records[i].push(record.slice(1));
					return;
				} else if (this.records[i][0] > record[0]) {
					this.records.splice(i, 0, [ record[0], '', record[1] ]);
					return;
				}
			}
			this.records.push([ record[0], '', record[1] ]);
		};

		Spreadsheet.prototype.toBlob = function() {
			var data = this.headers.join('\t') + '\n';
			$.each(this.records, function(i, record) {
				data += record.join('\t') + '\n';
			});
			return new Blob([ data ], { type: 'text/plain' });
		};

		return Spreadsheet;
	});

	app.controller('UserController', ['$scope', '$http', '$routeParams', 'User', function($scope, $http, $routeParams, User) {

		$scope.username = $routeParams.username;
		$scope.profile = null;

		$scope.isSelf = function() {
			return $scope.user && $scope.profile && $scope.profile.getName() === $scope.user.getName();
		};
		$scope.isAnon = function() {
			return $scope.user === null;
		};

		$scope.$watch('profile', function(profile) {
			if (profile) {
				$scope.page.setTitle(profile.getName());
			}
		});

		$scope.$watch('user', function(user) {
			if (angular.isDefined(user)) {
				if (user && $scope.username === user.getName()) {
					$scope.profile = user;
				} else if ($scope.username !== 'guest') {
					$http.get('/users/@' + $scope.username)
						.success(function(response) {
							$scope.profile = new User(response);
						})
						.error(function(response, status) {
							if (status < 500) {
								$scope.message = 'Can\'t retrieve this user.';
							} else {
								$scope.message = 'Couldn\'t retrieve this user. Try again later or contact support.';
							}
						}
					);
				}
			}
		});
	}]);

	app.controller('AccountSettingsController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {

		$scope.init = function() {
			$scope.message = '';
			$scope.email = $scope.profile.email;
			tracker.event('dialog', 'edit user');
		};

		$scope.data = function() {
			var data = {};
			if ($scope.email && $scope.email !== $scope.profile.email || !$scope.profile.verified) {
				data.email = $scope.email;
			}
			return data;
		};
		$scope.save = function() {
			$scope.alert.clear();
			var data = $scope.data();
			if (!$.isEmptyObject(data)) {
				$http.post('/users/@' + $scope.username, data)
					.success(function(response, status, headers) {
						$scope.alert.show('Updated account settings.', 'alert-success', headers('X-Command-ID'));
						$scope.closeDialog();
						$scope.whoami();
					})
					.error(function(response, status) {
						if (status < 500) {
							$scope.message = 'Can\'t save these changes.';
						} else {
							$scope.message = 'Couldn\'t save these changes. Try again later or contact support.';
						}
					});
			} else {
				$scope.cancel();
			}
			tracker.event('action', 'save user');
		};
		$scope.terminate = function() {
			if (confirm('Close your account and delete all associated data?')) {
				tracker.event('action', 'close account');
				$http({ method : 'DELETE', url : '/users/@' + $scope.username })
					.success(function() {
						$scope.closeDialog();
						$scope.signOut();
					})
					.error(function(response, status) {
						if (status < 500) {
							$scope.message = 'Can\'t close this account.';
						} else {
							$scope.message = 'Couldn\'t close this account. Try again later or contact support.';
						}
					});
			}
		};
	}]);

	app.controller('PlanDialogController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {

		$scope.init = function() {
			$scope.message = '';
			$scope.plan = $scope.user.quota;
			tracker.event('dialog', 'select plan');
		};

		$scope.select = function() {
			$scope.alert.clear();
			tracker.event('action', 'select plan', { 'Plan': $scope.plan });
		};
	}]);

	app.controller('SignInDialogController', ['$scope', '$http', '$location', '$route', 'User', 'token', 'tracker', function($scope, $http, $location, $route, User, token, tracker) {

		$scope.init = function() {
			$scope.message = '';
			$scope.username = '';
			$scope.password = '';
			tracker.event('dialog', 'sign in');
		};
		/** Ensures that autocompleted values are propagates to the model. */
		function update() {
			$scope.username = $("#sign-in-username").val();
			$scope.password = $("#sign-in-password").val();
		}
		$scope.signIn = function() {
			update();
			if ($scope.username.indexOf('@') != -1) {
				$scope.message = 'Please enter your username, not your email address.';
				return;
			}
			$http({ method: 'POST', url: '/oauth/token',
				data: $.param({ 'grant_type' : 'password', 'username' : $scope.username, 'password' : $scope.password }),
				headers: { 'Content-Type' : 'application/x-www-form-urlencoded' }
			})
				.success(function(response) {
					console.assert(response.access_token, 'missing token in sign in response');
					token.set(response.access_token);
					$scope.closeDialog();
					$scope.whoami();
					if ($location.url() === '/') {
						$location.url('/users/' + $scope.username);
					} else {
						$route.reload();
					}
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'The username or password you entered is incorrect.';
					} else {
						$scope.message = 'Unable to sign in, please try again later or contact support.';
					}
				});
			tracker.event('action', 'sign in');
		};

		$scope.$on('event:unauthorized', function() {
			$scope.openDialog('sign-in-dialog');
		});
	}]);

	app.controller('LostPasswordDialogController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {

		$scope.init = function() {
			$scope.username = '';
			$scope.email = '';
			$scope.message = '';
			tracker.event('dialog', 'password reset');
		};
		$scope.data = function() {
			return {
				username : $scope.username,
				email : $scope.email
			};
		};
		$scope.submit = function() {
			$scope.alert.clear();
			$http.post('/reset', $scope.data())
				.success(function() {
					$scope.alert.show('A password reset request has been sent by email. Check your inbox.');
					$scope.closeDialog();
					$scope.home();
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'The username and email address you entered don\'t match our records.';
					} else {
						$scope.message = 'Unable to reset your password, please try again later or contact support.';
					}
				});
			tracker.event('action', 'password reset');
		};
	}]);

	app.controller('SignUpDialogController', ['$scope', '$http', '$location', 'User', 'delay', 'tracker', function($scope, $http, $location, User, delay, tracker) {

		$scope.init = function() {
			$scope.username = '';
			$scope.password = '';
			$scope.passwordConfirmed = '';
			$scope.email = '';
			$scope.message = '';
			tracker.event('dialog', 'sign up');
		};
		$scope.data = function() {
			return {
				'username' : $scope.username,
				'password' : $scope.password,
				'email' : $scope.email
			};
		};
		$scope.submit = function() {
			$scope.alert.clear();
			$http.post('/users/', $scope.data())
				.success(function(response) {
					$scope.$parent.user = new User(response);
					$scope.closeDialog();
					delay(function() {
						$location.url('/users/' + $scope.$parent.user.name);
					});
				})
				.error(function(response, status) {
					if (status === 409) {
						$scope.message = 'The chosen username is not available.';
					} else {
						$scope.message = 'Unable to sign up, please try again later or contact support.';
					}
				});
			tracker.event('action', 'sign up');
		};
	}]);

	app.controller('UserVerificationController', ['$scope', '$http', '$location', '$routeParams', function($scope, $http, $location, $routeParams) {
		$http.post('/users/@' + $routeParams.username, { 'key' : $location.search()['key'], 'verified' : true })
			.success(function() {
				$scope.alert.show('Your email address has been verified.', 'alert-success');
				$scope.whoami();
				$location.url('/users/' + $routeParams.username);
			})
			.error(function() {
				$scope.alert.show('Your email address could not be verified.', 'alert-error');
				$location.url('/users/' + $routeParams.username);
			});
	}]);

	app.controller('PasswordResetController', ['$scope', '$http', '$location', '$routeParams', 'token', function($scope, $http, $location, $routeParams, token) {

		var username = $routeParams.username;
		var key = $location.search()['key'];
		var expires = $location.search()['expires'];

		$scope.init = function() {
			$scope.password = '';
			$scope.passwordConfirmed = '';
			$scope.message = '';
		};
		$scope.submit = function() {
			$scope.alert.clear();
			if ($scope.password !== $scope.passwordConfirmed) {
				$scope.message = 'Passwords don\'t match.';
				return;
			}
			$http.post('/users/@' + username, { 'key' : key, 'expires' : expires, 'password' : $scope.password })
				.success(function(response) {
					console.assert(response.access_token, 'missing access_token in password reset response');
					token.set(response.access_token);
					$scope.alert.show('Your password has been changed.', 'alert-success');
					$location.url('/users/' + username);
					$scope.whoami();
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Your password can\'t be changed.', 'alert-error');
					} else {
						$scope.alert.show('Your password could not be changed. Try again later or contact support.', 'alert-error');
					}
				});
		};

		$scope.init();
	}]);

	app.controller('OAuthController', ['$scope', '$http', '$location', '$window', 'token', function($scope, $http, $location, $window, token) {

		var getRedirectUri = function(params) {
			return [ $scope.redirectUri, $.param(params) ]
				.join(/[?#]$/.test($scope.redirectUri) ? '' : '#');
		};

		$scope.valid = function() {
			return $scope.client && $scope.redirectUri && $scope.bucket;
		};
		$scope.allow = function() {
			$scope.message = null;
			var data = $location.search();
			data.scope = $scope.bucket;
			$http.post('/oauth/authorize', data)
				.success(function(response) {
					console.assert(response.access_token, 'missing access_token in authorize response');
					console.assert(response.scope, 'missing scope in authorize response');
					$window.location = getRedirectUri(response);
				})
				.error(function(response) {
					if (response.error) {
						if (response.error == 'invalid_redirect_uri') {
							$scope.message = 'Redirect URI is not valid.';
						} else {
							$scope.deny(response.error, response.error_message);
						}
					} else {
						$scope.deny('server_error');
					}
				});
		};
		$scope.deny = function(code, message) {
			$window.location = getRedirectUri({ 'error' : code, 'error_message' : message });
		};

		$scope.client = $location.search()['client_id'];
		$scope.redirectUri = $location.search()['redirect_uri'];
		if (!$scope.client) {
			$scope.deny('invalid_request', 'client_id is missing');
		}
		if (!$scope.redirectUri) {
			$scope.message = 'Redirect URI is missing.';
		}
		if (!token.get()) {
			$scope.openDialog('sign-in-dialog');
		}

		$scope.$watch('user', function(user) {
			if (user) {
				$http.get('/users/' + $scope.user['@id'] + '/buckets/?' + $.param({ 'order' : 'label', 'offset' : 0, 'limit' : 100, 'label_only' : true }))
				.success(function(response) {
					$scope.buckets = response.buckets;
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t list buckets.';
					} else {
						$scope.message = 'Could not list buckets. Try again later or contact support.';
					}
				});
			} else {
				$scope.bucket = null;
			}
		});
	}]);

	app.controller('BucketListController', ['$scope', '$http', 'delay', 'taskRunner', 'tracker', function($scope, $http, delay, taskRunner, tracker) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.buckets = null;
		$scope.include_archived = false;

		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.offset + $scope.limit < $scope.total;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.limit });
		};
		$scope.includeArchived = function(includeArchived) {
			$scope.refresh({ include_archived : includeArchived });
		};
		$scope.params = function() {
			return {
				order : 'label',
				offset : $scope.offset,
				limit : $scope.limit,
				include_archived : $scope.include_archived
			};
		};
		$scope.refresh = function(params) {
			$http.get('/users/' + $scope.profile['@id'] + '/buckets/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					if (response.total === 0 && !$scope.include_archived) {
						$scope.refresh({ include_archived : true });
					} else {
						$scope.total = response.total;
						$scope.buckets = response.buckets;
					}
				});
		};
		$scope.loading = {};
		$scope.run = function(bucketId) {
			$scope.loading[bucketId] = true;
			$scope.alert.clear();
			taskRunner.runAll($scope, bucketId).then(function() {
				delay($scope.refresh);
			})['finally'](function() {
				delete $scope.loading[bucketId];
			});
			tracker.event('action', 'run tasks');
		};

		$scope.$watch('profile', function(profile) {
			if ($scope.isSelf() && profile) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);

	app.controller('CredentialsListController', ['$scope', '$http', 'tracker', 'delay', function($scope, $http, tracker, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.credentials = null;

		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.offset + $scope.limit < $scope.total;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.limit });
		};
		$scope.params = function() {
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/users/' + $scope.profile['@id'] + '/credentials/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.credentials = response.items;
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t retrieve any credentials.';
					} else {
						$scope.message = 'Couldn\'t retrieve any credentials. Try again later or contact support.';
					}
				});
		};
		$scope.remove = function(credentialsId) {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/credentials/' + credentialsId })
				.success(function(response, status, headers) {
					$scope.alert.show('Deleted credentials.', 'alert-success', headers('X-Command-ID'));
					$scope.offset = 0;
					delay($scope.refresh);
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t delete credentials.';
					} else {
						$scope.message = 'Couldn\'t delete credentials. Try again later or contact support.';
					}
				});
			tracker.event('action', 'delete credentials');
		};

		$scope.$watch('profile', function(profile) {
			if ($scope.isSelf() && profile) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);

	app.controller('AuthorizationListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.authorizations = null;

		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.offset + $scope.limit < $scope.total;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.limit });
		};
		$scope.params = function() {
			return {
				has_client : true,
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/users/' + $scope.profile['@id'] + '/authorizations/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.authorizations = response.authorizations;
				});
		};
		$scope.remove = function(authId) {
			$http({ method : 'DELETE', url : '/authorizations/' + authId })
				.success(function(response, status, headers) {
					$scope.alert.show('Revoked an authorization.', 'alert-success', headers('X-Command-ID'));
					$scope.offset = 0;
					delay($scope.refresh);
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Can\'t revoke the authorization.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t revoke the authorization. Try again later or contact support.', 'alert-error');
					}
				});
		};

		$scope.$watch('profile', function(profile) {
			if ($scope.isSelf() && profile) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);

	app.controller('HomeController', ['$scope', '$http', '$location', '$timeout', 'token', 'tracker', function($scope, $http, $location, $timeout, token, tracker) {

		$scope.start = function() {
			$scope.alert.clear();
			$http({ method: 'POST', url: '/oauth/token', data: 'grant_type=client_credentials',
				headers: { 'Content-Type' : 'application/x-www-form-urlencoded' }
			})
				.success(function(response) {
					console.assert(response.access_token, 'missing access_token in getting started response');
					token.set(response.access_token);
					$scope.whoami(function(user) {
						$location.path('/users/' + user.getName());
						$timeout(function() {
							$scope.openDialog('create-bucket-dialog');
						}, 1000);
					});
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Can\'t create a guest account.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t create a guest account. Try again later or contact support.', 'alert-error');
					}
				});
			tracker.event('action', 'get started');
		};
	}]);

	app.controller('CreateBucketDialogController', ['$scope', '$http', '$location', '$timeout', 'tracker', function($scope, $http, $location, $timeout, tracker) {

		$scope.init = function() {
			$scope.label = 'My Data';
			$scope.message = '';
			$scope.source = null;
			$scope.category = null;
			$scope.template = null;
			$http.get(cacheBuster.rewrite('/dashboard/templates.json')).success(function(response) {
				$scope.templates = response;
			});
			tracker.event('dialog', 'create bucket');
		};
		$scope.sources = function() {
			var sources = [];
			if ($scope.templates) {
				$.each($scope.templates, function(i, template) {
					if (!$scope.category || $scope.category === template.category) {
						if (sources.indexOf(template.source) === -1) {
							console.assert(template.source, template);
							sources.push(template.source);
						}
					}
				});
			}
			return sources.sort(function(a, b) {
				return a.localeCompare(b);
			});
		};
		$scope.categories = function() {
			var categories = [];
			if ($scope.templates) {
				$.each($scope.templates, function(i, template) {
					if (!$scope.source || $scope.source === template.source) {
						if (categories.indexOf(template.category) === -1) {
							console.assert(template.category, template);
							categories.push(template.category);
						}
					}
				});
			}
			return categories.sort();
		};
		$scope.valid = function() {
			return $scope.source && $scope.category || !$scope.source && !$scope.category;
		};
		$scope.validLabel = function() {
			return $scope.label && $scope.label.length > 0;
		};
		$scope.create = function() {
			$scope.alert.clear();
			$http.post('/buckets/', { label : $scope.label, widgets : $scope.template ? $scope.template.widgets : [] })
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.closeDialog();
					$location.url(location);
					if ($scope.template) {
						if ($scope.template.task) {
							$timeout(function() {
								$scope.openDialog('create-task-dialog', $scope.template.task);
							}, 500);
						} else if ($scope.template.importer) {
							$timeout(function() {
								$scope.openDialog('import-dialog', $scope.template.importer);
							}, 500);
						}
					}
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create bucket.';
					} else {
						$scope.message = 'Couldn\'t create bucket. Please try agan later or contact support.';
					}
				});
			tracker.event('action', 'create bucket', { 'Template': $scope.template ? $scope.template.label : undefined });
		};

		var setTemplate = function() {
			if ($scope.templates) {
				if ($scope.source && $scope.category) {
					$.each($scope.templates, function(i, template) {
						if (template.source === $scope.source && template.category === $scope.category) {
							$scope.template = template;
							return false;
						}
					});
				} else {
					$scope.template = null;
				}
			}
		};
		$scope.$watch('source', setTemplate);
		$scope.$watch('category', setTemplate);
	}]);

	app.controller('CreateViewDialogController', ['$scope', '$http', '$location', '$timeout', 'tracker', function($scope, $http, $location, $timeout, tracker) {

		$scope.init = function() {
			$scope.label = 'My View';
			$scope.message = '';
			$scope.buckets = [];
			$scope.aliases = [];
			$scope.selected = null;
			$scope.filter = null;
			$http.get('/users/' + $scope.profile['@id'] + '/buckets/?' + $.param({ order : 'label', offset : 0, limit : 100, labels_only : true })).success(function(response) {
				$scope.buckets = response.buckets;
			});
			tracker.event('dialog', 'create view');
		};
		$scope.valid = function() {
			return $scope.aliases.length > 0;
		};
		$scope.create = function() {
			$scope.alert.clear();
			var aliases = $.map($scope.aliases, function(alias) {
				return { '@id' : alias['@id'], 'filter' : alias.filter };
			});
			$http.post('/buckets/', { label : $scope.label, aliases : aliases })
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.closeDialog();
					$location.url(location);
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create view.';
					} else {
						$scope.message = 'Couldn\'t create view. Please try agan later or contact support.';
					}
				});
			tracker.event('action', 'create view');
		};
		$scope.listBuckets = function() {
			if ($scope.buckets) {
				return $.grep($scope.buckets, function(bucket) {
					return !bucket.aliases && $.grep($scope.aliases, function(alias) {
						return alias['@id'] == bucket['@id'];
					}).length === 0;
				});
			}
		};
		$scope.addBucket = function() {
			var alias = angular.copy($scope.selected);
			if ($scope.filter) {
				alias.filter = $scope.filter;
			}
			$scope.aliases.push(alias);
			$scope.selected = null;
			$scope.filter = null;
		};
		$scope.removeBucket = function(bucket) {
			$scope.aliases = $.grep($scope.aliases, function(alias) {
				return alias['@id'] != bucket['@id'];
			});
		};
	}]);

	app.factory('random', function() {
		return {
			id : function id() {
				var len = 5;
				var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
				var value = '';
				var pos;
				for (var i = 0; i < len; ++i) {
					pos = Math.floor(Math.random() * chars.length);
					value += chars.substring(pos, pos + 1);
				}
				return value;
			}
		};
	});

	app.controller('AddWidgetController', ['$scope', '$http', '$route', '$routeParams', '$location', '$timeout', 'random', function($scope, $http, $route, $routeParams, $location, $timeout, random) {

		$scope.dialog = $('#add-widget-dialog');
		$scope.templates = [
			{
				type : 'timeline',
				label : 'Timeline',
				description : 'Plots values on a timeline.',
				thumbnail : cacheBuster.rewrite('/img/widgets/timeline.png'),
				settings : { field : 'timestamp', statistic : 'count' }
			},
			{
				type : 'list',
				label : 'List',
				description : 'Shows all matching events, pageable.',
				thumbnail : cacheBuster.rewrite('/img/widgets/list.png'),
				settings : { limit : 10, order : '-timestamp' },
				singleton : true
			},
			{
				type : 'count',
				label : 'Count',
				description : 'Counts events by tag or author.',
				thumbnail : cacheBuster.rewrite('/img/widgets/count.png'),
				settings : { field : 'tag', order : '-count', limit : 5 }
			},
			{
				type : 'map',
				label : 'Map',
				description : 'Shows clusters of events on a map.',
				thumbnail : cacheBuster.rewrite('/img/widgets/map.png'),
				settings : { }
			},
			{
				type : 'heatmap',
				label : 'Map',
				description : 'Shows the density of events on a map.',
				thumbnail : cacheBuster.rewrite('/img/widgets/heatmap.png'),
				settings : { }
			},
			{
				type : 'ratings',
				label : 'Ratings',
				description : 'Counts events by their rating.',
				thumbnail : cacheBuster.rewrite('/img/widgets/ratings.png'),
				settings : { }
			},
			{
				type : 'histogram',
				label : 'Histogram',
				description : 'Shows the distribution of values in a field.',
				thumbnail : cacheBuster.rewrite('/img/widgets/histogram.png'),
				settings : { field : 'distance', interval : 10, unit : 'mi' }
			},
			{
				type : 'scoreboard',
				label : 'Scoreboard',
				description : 'Statistics for the values in a field.',
				thumbnail : cacheBuster.rewrite('/img/widgets/scoreboard.png'),
				settings : { key_field : 'author', value_field : 'distance', unit : 'mi', order : '-sum', limit : 10 }
			},
			{
				type : 'gantt',
				label : 'Frequency',
				description : 'Shows how long ago and how often certain events occur.',
				thumbnail : cacheBuster.rewrite('/img/widgets/gantt.png'),
				settings : { field : 'tag', order : '-max', limit : 10 }
			},
			{
				type : 'polar',
				label : 'Polar Chart',
				description : 'Plots values by month of year, day of week, or hour of day.',
				thumbnail : cacheBuster.rewrite('/img/widgets/polar.png'),
				settings : { interval : 'day_of_week', value_field : 'timestamp' }
			},
			{
				type : 'scatterplot',
				label : 'Scatter Plot',
				description : 'Correlates values from two fields.',
				thumbnail : cacheBuster.rewrite('/img/widgets/scatterplot.png'),
				settings : { field_x : 'count', field_y : 'count' }
			},
			{
				type : 'sonification',
				label : 'Sonify',
				description : 'Plays data from timeline widgets as sounds.',
				thumbnail : cacheBuster.rewrite('/img/widgets/sonification.png'),
				settings : { tempo : 176, scale : 'chromatic' },
				singleton : true
			}
		];
		$scope.init = function(placement) {
			$scope.placement = placement;
		};
		$scope.add = function(template) {
			var settings = {
				'id' : random.id(),
				'type' : template.type,
				'label' : template.label,
				'placement' : $scope.placement
			};
			$.extend(true, settings, template.settings);
			$scope.addWidget(settings);
			$timeout(function() {
				$('#' + settings.id + '-tab').tab('show');
				$scope.openDialog(settings.id + '-dialog');
				$scope.setDirty(true);
			}, 500);
		};
		$scope.findTemplates = function() {
			return $.grep($scope.templates, function(template) {
				return !template.singleton || !$scope.exists(template);
			});
		};
		$scope.exists = function(template) {
			return $scope.bucket && $.grep($scope.bucket.widgets, function(widget) {
				return widget.type === template.type;
			}).length > 0;
		};
	}]);


	app.factory('Bucket', [ '$http', function($http) {

		var Bucket = function(data) {
			$.extend(this, data);
		};

		Bucket.getLabel = function(id, callback) {
			$http.get('/buckets/' + id + '/label')
				.success(function(response) {
					callback(response.label);
				});
		};

		Bucket.prototype.getLabel = function() {
			return this.label || '?';
		};

		Bucket.prototype.isPublished = function() {
			return $.grep(this.roles, function(role) {
				return role.principal === '*';
			}).length > 0;
		};

		Bucket.prototype.publish = function() {
			if (!this.isPublished()) {
				this.roles.push({ 'principal' : '*', 'role' : 'viewer' });
			}
		};

		Bucket.prototype.unpublish = function() {
			this.roles = $.grep(this.roles, function(role) {
				return role.principal !== '*';
			});
		};

		Bucket.prototype.getOwner = function() {
			for (var i = 0, max = this.roles.length; i < max; ++i) {
				if (this.roles[i].role === 'owner') {
					return this.roles[i].principal;
				}
			}
		};

		Bucket.prototype.canEdit = function(principal) {
			for (var i = 0; i < this.roles.length; ++i) {
				if (this.roles[i].principal === principal) {
					return this.roles[i].role === 'owner' || this.roles[i].role === 'contributor';
				}
			}
		};

		Bucket.prototype.isVirtual = function() {
			return this.aliases && this.aliases.length > 0;
		};

		return Bucket;
	}]);

	app.controller('DashboardController', ['$scope', '$http', '$route', '$routeParams', '$location', '$q', '$window', 'Bucket', 'Field', 'Constraint', 'tracker', 'delay', 'taskRunner', function($scope, $http, $route, $routeParams, $location, $q, $window, Bucket, Field, Constraint, tracker, delay, taskRunner) {

		function updateEditable() {
			$scope.editable = $scope.user && $scope.bucket.canEdit($scope.user['@id']);
		}

		$scope.bucketId = $routeParams.bucketId;
		$http.get('/buckets/' + $scope.bucketId)
			.success(function(response) {
				$scope.bucket = new Bucket(response);
				$scope.page.setTitle($scope.bucket.label);
				$scope.$watch('user', updateEditable);
			})
			.error(function(response, status) {
				if (status < 500) {
					$scope.message = 'Can\'t retrieve this bucket.';
				} else {
					$scope.message = 'Couldn\'t retrieve this bucket. Try again later or contact support.';
				}
			});

		$scope.constraints = [];
		$scope.constraintsB = [];
		$scope.widgets = [];

		var layout = {};
		$scope.$watch('bucket.widgets', function() {
			var l = {};
			if ($scope.bucket) {
				$.each($scope.bucket.widgets, function(i, widget) {
					l[widget.placement] = true;
				});
			}
			layout = l;
		});
		$scope.hasWidgets = function(placement) {
			return layout[placement];
		};

		$scope.getWidgetSettings = function(placement) {
			return $scope.bucket && $.grep($scope.bucket.widgets, function(widget) {
				return widget.placement === placement;
			});
		};
		$scope.removeWidget = function(settings) {
			if ($scope.bucket.widgets.length > 1) {
				delay(function() { // dialog won't close properly if we don't delay
					$scope.bucket.widgets = $.grep($scope.bucket.widgets, function(widget) {
						return widget.id !== settings.id;
					});
					$scope.widgets = $.grep($scope.widgets, function(widget) {
						return widget.settings.id !== settings.id;
					});
					var remaining = $scope.getWidgetSettings(settings.placement);
					if (remaining.length > 0) {
						$('#' + remaining[0].id + '-tab').tab('show');
					}
					$scope.setDirty(true);
				});
			}
		};
		$scope.canImport = function() {
			return typeof FileReader != 'undefined' && $scope.editable;
		};
		$scope.addWidget = function(settings) {
			$scope.bucket.widgets.push(settings);
		};
		$scope.moveWidget = function(sourceId, targetId) {
			var sourceWidget, sourceIndex;
			$.each($scope.bucket.widgets, function(i, widget) {
				if (widget.id === sourceId) {
					sourceIndex = i;
					sourceWidget = widget;
					return false;
				}
			});
			console.assert(sourceWidget, "missing source widget", sourceId);
			$scope.bucket.widgets.splice(sourceIndex, 1);
			if (targetId.charAt(0) === '+') {
				sourceWidget.placement = targetId.substring(1);
				$scope.bucket.widgets.push(sourceWidget);
			} else {
				$.each($scope.bucket.widgets, function(i, widget) {
					if (widget.id === targetId) {
						sourceWidget.placement = widget.placement;
						$scope.bucket.widgets.splice(i, 0, sourceWidget);
						return false;
					}
				});
			}
			$scope.refresh();
		};
		$scope.getTemplate = function(type) {
			return cacheBuster.rewrite('/dashboard/' + type + '.html');
		};

		var register = function(widget) {
			$scope.widgets.push(widget);
		};
		var seen = 0;
		$scope.register = function(widget, implicit) {
			register(widget);
			if (!implicit) {
				if (++seen == $scope.bucket.widgets.length) {
					$scope.register = register;
					$scope.refresh();
				}
			}
		};

		function search(q, facets) {
			return $http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'facet' : facets }, true));
		}
		/**
		 * Escape commas.
		 */
		function escape(s) {
			return typeof s === 'string' ? s.replace(/,/g, '\\,') : s;
		}
		$scope.search = function(params, callback) {
			var facets = $.map(params, function(param) {
				return $.map(param, function(value, key) {
					return angular.isDefined(value) && value !== null && value !== '' ?
							key + ':' + escape(value) : null;
				}).join(',');
			});
			var t0 = new Date().getTime();
			var requests = [ search($scope.constraints, facets) ];
			if ($scope.constraintsB.length > 0) {
				requests.push(search($scope.constraintsB, facets));
			}
			$q.all(requests).then(function(responses) {
				var t1 = new Date().getTime();
				callback(responses[0].data, responses.length > 1 ? responses[1].data : null);
			}, function() {
				callback({ total : -1 });
			});
		};
		$scope.refresh = function() {
			$scope.updateConstraints();
			var params = $.map($scope.widgets, function(widget) { return widget.params(); });
			$scope.$broadcast('refresh');
			$scope.search(params, function(response, responseB) {
				$scope.total = response.total;
				$scope.$broadcast('result', response, responseB);
			});
		};
		$scope.removeEvent = function(eventId) {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/buckets/' + $scope.bucketId + '/' + eventId })
				.success(function(response, status, headers) {
					delay($scope.refresh);
					$scope.alert.show('Deleted an event.', 'alert-success', headers('X-Command-ID'));
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Can\'t delete the event.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t delete the event. Try again later or contact support.', 'alert-error');
					}
				});
			tracker.event('action', 'delete event');
		};
		$scope.run = function() {
			$scope.alert.clear();
			$scope.loading = true;
			taskRunner.runAll($scope, $scope.bucketId).then(function() {
				delay($scope.refresh);
			})['finally'](function() {
				$scope.loading = false;
			});
			tracker.event('action', 'run tasks');
		};

		$scope.$on('$routeUpdate', function() {
			$scope.refresh();
		});
		$scope.$on('credentials', function() {
			$scope.run();
		});

		function parseConstraints(value) {
			if (value && !$.isArray(value)) {
				value = value.split('|');
			}
			return value ? $.map(value, function(s) { return Constraint.parse(s); }) : [];

		}
		$scope.updateConstraints = function() {
			$scope.constraints = parseConstraints($location.search()['q']);
			$scope.constraintsB = parseConstraints($location.search()['r']);
		};
		$scope.getConstraints = function(field) {
			return $.grep($scope.constraints, function(constraint) {
				return constraint.field === field;
			});
		};
		$scope.getConstraintsB = function(field) {
			return $.grep($scope.constraintsB, function(constraint) {
				return constraint.field === field;
			});
		};
		$scope.getConstraintsString = function() {
			var items = mapToString($scope.constraints);
			return items !== null ? items.join('|') : null;
		};
		function containsConstraint(constraint) {
			return $.grep($scope.constraints, function(c) {
				return angular.equals(c, constraint);
			}).length > 0;
		}
		function mapToString(values) {
			return values.length > 0 ?
				$.map(values, function(value) { return value.toString(); }) :
				null;
		}
		function params() {
			var value = {};
			if ($scope.constraints.length > 0) {
				value.q = mapToString($scope.constraints);
			}
			if ($scope.constraintsB.length > 0) {
				value.r = mapToString($scope.constraintsB);
			}
			return value;
		}
		$scope.addConstraint = function(field, value, replace, negated) {
			var subfield = null;
			var p = field.indexOf('$');
			if (p > 0) {
				subfield = field.substring(p + 1);
				field = field.substring(0, p);
			}
			var constraint = new Constraint(field, value, negated, subfield);
			if (containsConstraint(constraint)) {
				return;
			}
			if (replace) {
				$scope.constraints = $.grep($scope.constraints, function(c) {
					return c.field !== constraint.field;
				});
			}
			$scope.constraints.push(constraint);
			$location.search(params());
		};
		$scope.addConstraints = function(constraints) {
			$scope.constraints = $scope.constraints.concat(constraints);
			$location.search(params());
		};
		$scope.removeConstraint = function(constraint) {
			$scope.constraints = $.grep($scope.constraints, function(c) {
				return !angular.equals(c, constraint);
			});
			$location.search(params());
		};
		$scope.removeConstraintB = function(constraint) {
			$scope.constraintsB = $.grep($scope.constraintsB, function(c) {
				return !angular.equals(c, constraint);
			});
			$location.search(params());
		};
		$scope.invertConstraint = function(constraint) {
			$scope.constraints = $.map($scope.constraints, function(c) {
				return angular.equals(c, constraint) ? c.invert() : c;
			});
			$location.search(params());
		};
		$scope.invertConstraintB = function(constraint) {
			$scope.constraintsB = $.map($scope.constraintsB, function(c) {
				return angular.equals(c, constraint) ? c.invert() : c;
			});
			$location.search(params());
		};
		$scope.getConstraintIcon = function(constraint) {
			var fieldName = constraint.field;
			var dot = fieldName.indexOf('.');
			if (dot != -1) {
				fieldName = fieldName.substring(0, dot);
			}
			var field = Field.find(fieldName);
			return field ? field.icon : 'fa-circle';
		};
		$scope.swapAB = function() {
			var tmp = $scope.constraints;
			$scope.constraints = $scope.constraintsB.length ? $scope.constraintsB : angular.copy($scope.constraints);
			$scope.constraintsB = tmp;
			$location.search(params());
			tracker.event('action', 'A/B test');
		};

		$scope.dirty = false;
		$scope.setDirty = function(dirty) {
			$scope.dirty = dirty;
		};

		var clock = 0;
		$scope.tic = function() {
			$scope.$broadcast('tic', clock++, true);
		};
		$scope.untic = function() {
			$scope.$broadcast('tic', clock, false);
			clock = 0;
		};
	}]);

	app.controller('EditWidgetsController', ['$scope', '$http', '$route', 'tracker', function($scope, $http, $route, tracker) {
		$scope.save = function() {
			$scope.alert.clear();
			$http.put('/buckets/' + $scope.bucketId, $scope.bucket)
				.success(function (response, status, headers) {
					$scope.alert.show('Saved settings.', 'alert-success', headers('X-Command-ID'));
					++$scope.$parent.bucket.version;
					$scope.setDirty(false);
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.alert.show('Can\'t save this bucket', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t save this bucket. Try again later or contact support.', 'alert-error');
					}
				});
			tracker.event('action', 'save widgets');
		};
		$scope.revert = function() {
			$route.reload();
		};
	}]);

	app.controller('SaveAsViewDialogController', ['$scope', '$http', '$location', '$timeout', 'tracker', function($scope, $http, $location, $timeout, tracker) {

		$scope.init = function() {
			$scope.label = $scope.$parent.bucket.label;
			$scope.message = '';
			tracker.event('dialog', 'save as view');
		};
		$scope.create = function() {
			var bucket = {
				'label' : $scope.label,
				'widgets' : $scope.$parent.bucket.widgets,
				'aliases' : [
					{
						'@id' : $scope.bucket['@id'],
						'filter' : $scope.$parent.getConstraintsString()
					}
				]
			};
			$http.post('/buckets/', bucket)
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.alert.clear();
					$scope.closeDialog();
					$location.url(location);
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create view.';
					} else {
						$scope.message = 'Couldn\'t create view. Please try agan later or contact support.';
					}
				});
			tracker.event('action', 'create view');
		};
	}]);

	app.controller('EditBucketDialogController', ['$scope', '$http', '$location', 'delay', 'tracker', function($scope, $http, $location, delay, tracker) {

		$scope.init = function() {
			$scope.newBucket = angular.copy($scope.$parent.bucket);
			$scope.newBucket.refresh = $scope.newBucket.refresh && $scope.user.quota ? true : false;
			$scope.isView = $scope.newBucket.aliases && $scope.newBucket.aliases.length > 0;
			$scope.selected = null;
			$scope.filter = null;
			$http.get('/users/' + $scope.user['@id'] + '/buckets/?' + $.param({ 'order' : 'label', offset : 0, limit : 100, labels_only : true })).success(function(response) {
				$scope.buckets = response.buckets;
			});
			tracker.event('dialog', 'edit bucket');
		};
		$scope.listBuckets = function() {
			if ($scope.buckets) {
				return $.grep($scope.buckets, function(bucket) {
					return !bucket.aliases && $.grep($scope.newBucket.aliases, function(alias) {
						return alias['@id'] == bucket['@id'];
					}).length === 0;
				});
			}
		};
		$scope.addBucket = function() {
			$scope.newBucket.aliases.push({ '@id' : $scope.selected['@id'], 'filter' : $scope.filter });
			$scope.selected = null;
			$scope.filter = null;
		};
		$scope.removeBucket = function(bucketId) {
			$scope.newBucket.aliases = $.grep($scope.newBucket.aliases, function(bucket) {
				return bucket['@id'] !== bucketId;
			});
		};
		$scope.valid = function() {
			return !$scope.isView || $scope.newBucket.aliases.length > 0;
		};
		$scope.save = function() {
			$scope.message = '';
			$http.put('/buckets/' + $scope.bucketId, $scope.newBucket)
				.success(function(response, status, headers) {
					$scope.closeDialog();
					$scope.alert.show('Saved settings.', 'alert-success', headers('X-Command-ID'));
					$scope.newBucket.version += 1;
					$scope.$parent.bucket = $scope.newBucket;
					delay($scope.refresh);
					tracker.event('action', 'save bucket');
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t save this bucket';
					} else {
						$scope.message = 'Couldn\'t save this bucket. Try again later or contact support.';
					}
				});
		};
		$scope.archiveBucket = function(archive) {
			$scope.alert.clear();
			if (archive) {
				$scope.newBucket.archived = true;
			} else {
				delete $scope.newBucket.archived;
			}
			$http.put('/buckets/' + $scope.bucketId, $scope.newBucket)
				.success(function() {
					$scope.closeDialog();
					$location.url('/users/' + $scope.$parent.user.getName());
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t archive this bucket.';
					} else {
						$scope.message = 'Couldn\'t archive this bucket. Try again later or contact support.';
					}
				});
			tracker.event('action', 'delete bucket');
		};
		$scope.deleteBucket = function() {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/buckets/' + $scope.bucketId })
				.success(function() {
					$scope.closeDialog();
					$location.url('/users/' + $scope.$parent.user.getName());
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t delete this bucket.';
					} else {
						$scope.message = 'Couldn\'t delete this bucket. Try again later or contact support.';
					}
				});
			tracker.event('action', 'delete bucket');
		};
	}]);

	app.factory('WidgetControllerSupport', function() {
		return function($scope) {
			$scope.classesForOrderBy = function(column) {
				var classes = [];
				if ($scope.settings.order && $scope.settings.order.indexOf(column) !== -1) {
					classes.push('fa');
					classes.push($scope.settings.order.charAt(0) === '-' ? 'fa-sort-desc' : 'fa-sort-asc');
				}
				return classes;
			};
		};
	});

	app.factory('WidgetDialogControllerSupport', ['Field', function(Field) {
		return function($scope, orderBy) {
			$scope.init = function() {
				$scope.settings = angular.copy($scope.$parent.settings);
			};
			$scope.save = function() {
				$scope.refresh({}, $scope.settings);
				$scope.closeDialog();
				$scope.setDirty(true);
			};
			$scope.getField = function(name) {
				return Field.find(name);
			};
			if (orderBy) {
				$scope.orderBy = orderBy;
				$scope.ascDesc = { 'asc' : true, 'desc' : false };
				$scope.asc = function(asc) {
					if (angular.isDefined(asc)) {
						if (!asc && $scope.asc()) {
							$scope.settings.order = '-' + $scope.settings.order;
						} else if (asc && !$scope.asc()) {
							$scope.settings.order = $scope.settings.order.substr(1);
						}
					}
					return $scope.settings.order && $scope.settings.order.charAt(0) !== '-';
				};
				$scope.order = function(order) {
					if (angular.isDefined(order)) {
						if (!$scope.asc()) {
							order = '-' + order;
						}
						$scope.settings.order = order;
					}
					return $scope.settings.order && $scope.settings.order.charAt(0) === '-' ?
						$scope.settings.order.substr(1) : $scope.settings.order;
				};
			}
		};
	}]);


	app.factory('WidgetFilter', ['Constraint', function(Constraint) {

		var WidgetFilter = function(fields) {
			this.fields = fields;
			this.field = fields[0];
			this.value = null;
		};

		WidgetFilter.prototype.clear = function() {
			this.value = null;
		};

		WidgetFilter.prototype.build = function() {
			var field = this.field.id;
			var values = [];
			if (this.value) {
				if (this.field.tokenized) {
					var r = /([^"]\S*|".+?")\s*/g;
					var m;
					while (m = r.exec(this.value)) {
						values.push(m[1]);
					}
				} else {
					values = [ this.value ];
				}
			}
			return $.map(values, function (value) {
				return new Constraint(field, value);
			});
		};

		return WidgetFilter;
	}]);


	app.controller('ListWidgetController', ['$scope', 'WidgetFilter', function($scope, WidgetFilter) {

		$scope.init = function() {
			$scope.offset = 0;
			$scope.total = 0;
			$scope.items = null;
		};
		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.offset + $scope.settings.limit < $scope.total;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.settings.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.settings.limit });
		};
		$scope.filter = new WidgetFilter([
			{
				id : 'resource.title',
				label : 'resources',
				icon : 'fa-bookmark',
				tokenized : true
			}, {
				id : 'source.title',
				label : 'sources',
				icon : 'fa-external-link',
                tokenized : true
			}, {
				id : 'tag',
				label : 'tags',
				icon : 'fa-tag',
                tokenized : false
			}, {
				id : 'note',
				label : 'notes',
				icon : 'fa-comment-o',
                tokenized : true
			}
		]);
		$scope.applyFilter = function() {
			$scope.addConstraints($scope.filter.build());
			$scope.filter.clear();
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'list',
				offset : 0,
				limit : $scope.settings.limit,
				order : $scope.settings.order,
				filter : $scope.filter.build().join('|')
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$scope.init();
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.total = result.total;
			$scope.items = result[$scope.settings.id] || [];
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$scope.$watch('filter', function(to, from) {
			if (from !== to) {
				$scope.refresh({ offset : 0 });
			}
		}, true);
	}]);

	app.controller('ListWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		var fields = [ 'timestamp' ];
		$.each(Field.findByType('numeric'), function(i, field) {
			fields.push(field.name);
		});

		new WidgetDialogControllerSupport($scope, fields);
	}]);

	app.controller('CountWidgetController', ['$scope', 'WidgetControllerSupport', function($scope, WidgetControllerSupport) {

		new WidgetControllerSupport($scope);

		$scope.init = function() {
			$scope.offset = 0;
			$scope.more = false;
			$scope.terms = null;
		};
		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.more;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.settings.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.settings.limit });
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'count',
				field : $scope.settings.field,
				offset : $scope.offset,
				limit : $scope.settings.limit,
				order : $scope.settings.order,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			var terms = result[$scope.settings.id] || [];
			$scope.more = terms.length > $scope.settings.limit;
			$scope.terms = $scope.more ? terms.slice(0, $scope.settings.limit) : terms;
		};
		$scope.filter = function(term) {
			$scope.offset = 0;
			$scope.addConstraint($scope.settings.field, term.label);
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('CountWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope, [ 'term', 'count' ]);

		$scope.fields = Field.findByType('text');
	}]);

	app.controller('GanttWidgetController', ['$scope', 'WidgetControllerSupport', 'timezone', function($scope, WidgetControllerSupport, timezone) {

		new WidgetControllerSupport($scope);

		$scope.keyField = 'timestamp';

		$scope.init = function() {
			$scope.terms = null;
			$scope.settings.key_field = $scope.settings.key_field || $scope.keyField;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'gantt',
				key_field : $scope.settings.key_field,
				field : $scope.settings.field,
				timezone : timezone,
				order : $scope.settings.order,
				limit : $scope.settings.limit,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.terms = result[$scope.settings.id] || [];
			if ($scope.terms) {
				$.each($scope.terms, function(i, term) {
					term.freq = Math.round((new Date(term.last).getTime() - new Date(term.first).getTime()) / (term.count - 1));
				});
			}
		};
		$scope.filter = function(term) {
			$scope.addConstraint($scope.settings.field, term.label);
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('GanttWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope, [ 'term', 'max' ]);

		$scope.fields = Field.findByType('text');
		$scope.subfields = $.map(Field.find($scope.keyField).subfields, function(subfield) {
			return { label : subfield, value : (subfield ? $scope.keyField + '$' + subfield : $scope.keyField) };
		});
	}]);

	app.controller('RatingsWidgetController', ['$scope', function($scope) {

		$scope.field = 'rating';

		$scope.init = function() {
			$scope.ratings = null;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'ratings',
				filter : $scope.settings.filter
			};
		};
		$scope.update = function(event, result) {
			$scope.ratings = result[$scope.settings.id] || [];
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result);
			});
		};
		function toString(value) {
			return typeof value === 'number' ? value + '%' : '*';
		}
		$scope.filter = function(rating) {
			$scope.offset = 0;
			$scope.addConstraint($scope.field, '[' + toString(rating.from) + '..' + toString(rating.to) + ')');
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('RatingsWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', function($scope, WidgetDialogControllerSupport) {
		new WidgetDialogControllerSupport($scope);
	}]);

	app.controller('HistogramWidgetController', ['$scope', '$timeout', 'Field', 'Spreadsheet', function($scope, $timeout, Field, Spreadsheet) {

		$scope.init = function() {
			$scope.intervals = null;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'histogram',
				field : $scope.settings.field,
				interval : $scope.settings.interval,
				unit : $scope.settings.unit,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result) {
			$scope.intervals = result[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.snapshot = function() {
			$scope.$broadcast('snapshot');
		};
		$scope.toSpreadsheet = function() {
			var spreadsheet = new Spreadsheet([ $scope.settings.field, 'count' ]);
			var field = Field.find($scope.settings.field);
			$.each($scope.intervals, function(i, interval) {
				var value = '[' + field.toText(interval.from) + '..' + field.toText(interval.to) + ')';
				spreadsheet.addRecord([ value, interval.count ]);
			});
			return spreadsheet;
		};
		$scope.draw = function() {
			if ($scope.intervals && $scope.intervals.length) {
				var field = Field.find($scope.settings.field);
				var height = Math.max($scope.intervals.length * 20, 150);
				if ($scope.intervalsB && $scope.intervalsB.length) {
					height *= 2;
				}
				var options = {
					chart : {
						type : 'bar',
						zoomType : 'x',
						height : height,
						animation : false,
						events : {
							selection : function(event) {
								var min = (event.xAxis[0].min !== undefined) ? Math.ceil(event.xAxis[0].min) : 0;
								var max = (event.xAxis[0].max !== undefined) ? Math.floor(event.xAxis[0].max) : $scope.intervals.length - 1;
								if (min <= max) {
									var from = field.toText($scope.intervals[max].from);
									var to = field.toText($scope.intervals[min].to);
									if (from || to) {
										var range = '[' + from + '..' + to + ')';
										$scope.$apply(function() {
											$scope.addConstraint($scope.settings.field, range, true);
										});
									}
								}
								return false;
							}
						}
					},
					title : {
						text : null
					},
					xAxis : {
						categories : [],
						tickLength : 0
					},
					yAxis : {
						title : null,
						labels : {
							overflow : 'justify'
						},
						allowDecimals : false
					},
					series : [{
						name : 'count',
						color : 'rgba(47, 126, 216, 0.4)',
						data : []
					}],
					tooltip : {
						shared : false,
						hideDelay : 0,
						crosshairs : false,
						headerFormat : '<b>{point.key}</b>: ',
						pointFormat : '{point.y}'
					},
					plotOptions : {
						series : {
							pointWidth : 10,
							borderRadius : 5,
							borderWidth : 0,
							cursor : 'pointer',
							animation : false,
							events : {
								click : function(event) {
									var interval = $scope.intervals[event.point.x];
									var range = '[' + field.toText(interval.from) + '..' + field.toText(interval.to) + ')';
									$scope.$apply(function() {
										$scope.addConstraint($scope.settings.field, range, true);
									});
								}
							}
						}
					},
					legend : {
						enabled : false
					},
					credits: {
						enabled: false
					}
				};
				$.each($scope.intervals, function(i, interval) {
					options.xAxis.categories.push(field.toText(interval.from) + '..' + field.toText(interval.to));
					options.series[0].data.push(interval.count);
				});
				$scope.chartOptions = options;
			}
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('HistogramWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope);

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0 ?
					$scope.settings.unit === null :
					$.inArray($scope.settings.unit, units) != -1;
		}

		$scope.getField = function() {
			return Field.find($scope.settings.field);
		};
		$scope.getFields = function() {
			return Field.findByType('numeric');
		};
		$scope.getUnits = function() {
			var f = Field.find($scope.settings.field);
			return f ? f.units : [];
		};
		$scope.valid = function() {
			return $scope.settings.interval > 0.0 && isUnitValid();
		};
		$scope.$watch('settings.field', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
		});
	}]);

	app.controller('ScoreboardWidgetController', ['$scope', 'WidgetControllerSupport', function($scope, WidgetControllerSupport) {

		new WidgetControllerSupport($scope);

		$scope.init = function() {
			$scope.terms = null;
			if (!$scope.settings.statistics) {
				$scope.settings.statistics = [ 'count', 'sum', 'avg' ];
			}
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'scoreboard',
				key_field : $scope.settings.key_field,
				value_field : $scope.settings.value_field,
				unit : $scope.settings.unit,
				order : $scope.settings.order,
				limit : $scope.settings.limit,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.terms = result[$scope.settings.id] || [];
		};
		$scope.filter = function(term) {
			$scope.addConstraint($scope.settings.key_field, term.label);
		};
		$scope.selected = function(value) {
			return $scope.settings.statistics.indexOf(value) != -1;
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('ScoreboardWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope, [ 'term', 'count', 'sum', 'min', 'max', 'avg' ]);

		$scope.isUnitValid = function() {
			var units = $scope.getUnits();
			return units.length === 0 ?
				$scope.settings.unit === null :
				$.inArray($scope.settings.unit, units) != -1;
		};
		$scope.getKeyFields = function() {
			return Field.findByType('text');
		};
		$scope.getValueFields = function() {
			return Field.findByType('numeric');
		};
		$scope.getUnits = function() {
			var valueField = Field.find($scope.settings.value_field);
			return valueField ? valueField.units : [];
		};
		$scope.toggle = function(value) {
			var i = $scope.settings.statistics.indexOf(value);
			if (i != -1) {
				$scope.settings.statistics.splice(i, 1);
			} else {
				$scope.settings.statistics.push(value);
			}
		};

		$scope.$watch('settings.value_field', function() {
			if (!$scope.isUnitValid()) {
				$scope.settings.unit = null;
			}
		});
	}]);

	app.factory('Interval', function() {

		var Interval = function(name, pattern, minTickInterval, symbol) {
			this.name = name;
			this.pattern = pattern.length;
			this.minTickInterval = minTickInterval;
			this.symbol = symbol;
		};

		Interval.VALUES = [
			new Interval('year', 'yyyy', 366 * 24 * 60 * 60 * 1000, 'y'),
			new Interval('month', 'yyyy-MM', 28 * 24 * 60 * 60 * 1000, 'M'),
			new Interval('week', 'yyyy-Www', 7 * 24 * 60 * 60 * 1000, 'w'),
			new Interval('day', 'yyyy-MM-dd', 24 * 60 * 60 * 1000, 'd'),
			new Interval('hour', 'yyyy-MM-ddTHH', 60 * 60 * 1000, 'h'),
			new Interval('minute', 'yyyy-MM-ddTHH:mm', 60 * 1000, 'm'),
			new Interval('second', 'yyyy-MM-ddTHH:mm:ss', 1000, 's')
		];

		Interval.VALUES[0].zoomIn = Interval.VALUES[1]; // year -> month
		Interval.VALUES[1].zoomIn = Interval.VALUES[3]; // month -> day
		Interval.VALUES[2].zoomIn = Interval.VALUES[3]; // week -> day
		Interval.VALUES[3].zoomIn = Interval.VALUES[4]; // day -> hour
		Interval.VALUES[4].zoomIn = Interval.VALUES[5]; // hour -> minute
		Interval.VALUES[5].zoomIn = Interval.VALUES[6]; // minute -> second

		Interval.match = function(value) {
			if (value.match(/^[0-9]{4}/)) {
				if (!value.match(/Z|[+-]\d\d:\d\d/)) {
					var i, max;
					for (i = 1, max = Interval.VALUES.length; i < max; ++i) {
						if (value.length === Interval.VALUES[i].pattern) {
							return Interval.VALUES[i].zoomIn;
						}
					}
				}
			}
		};

		function getFirst(rangeExpression) {
			if (rangeExpression.length >= 12 && rangeExpression.indexOf('..') != -1) {
				var tokens = rangeExpression.substring(1, rangeExpression.length - 1).split('..');
				if (tokens[0] == '*') {
					return tokens[1];
				}
				if (tokens[1] == '*') {
					return tokens[0];
				}
				return tokens[0];
			}
		}

		Interval.matchRange = function(value) {
			value = getFirst(value);
			if (value && value.match(/^[0-9]{4}/)) {
				if (!value.match(/Z|[+-]\d\d:\d\d/)) {
					var i, max;
					for (i = 0, max = Interval.VALUES.length; i < max; ++i) {
						if (value.length === Interval.VALUES[i].pattern) {
							return Interval.VALUES[i];
						}
					}
				}
			}
		};

		Interval.matchSymbol = function(value) {
			if (value) {
				var i, max;
				for (i = 0, max = Interval.VALUES.length; i < max; ++i) {
					if (value.indexOf(Interval.VALUES[i].symbol) != -1) {
						return Interval.VALUES[i];
					}
				}
			}
		};

		Interval.valueOf = function(name) {
			if (name) {
				var i, max;
				for (i = 0, max = Interval.VALUES.length; i < max; ++i) {
					if (Interval.VALUES[i].name === name) {
						return Interval.VALUES[i];
					}
				}
			}
		};

		return Interval;
	});

	app.controller('TimelineWidgetController', ['$scope', '$timeout', 'Field', 'Interval', 'Spreadsheet', 'moment', 'statistics', function($scope, $timeout, Field, Interval, Spreadsheet, moment, statistics) {

		$scope.keyField = 'timestamp';

		function commonPrefix(a, b) {
			if (!a) {
				return '';
			}
			if (!b) {
				return a;
			}
			var i = 0;
			var at = a.split(/(?=[-T:Z]+)/);
			var bt = b.split(/(?=[-T:Z]+)/);
			while (i < at.length && i < bt.length) {
				if (at[i] !== bt[i]) {
					break;
				}
				++i;
			}
			return at.slice(0, i).join('');
		}
		function filter(value) {
			$scope.addConstraint($scope.settings.key_field, value, true);
		}

		$scope.init = function() {
			$scope.times = null;
			$scope.timesB = null;
			$scope.paired = false;
			$scope.settings.key_field = $scope.settings.key_field || $scope.keyField;
		};
		$scope.params = function() {
			$scope.interval = Interval.valueOf($scope.settings.interval) || Interval.VALUES[1];
			$scope.range = '';
			var q = '';
			$.each($scope.getConstraints($scope.keyField), function(i, constraint) {
				q = constraint.value;
			});
			var r = '';
			$.each($scope.getConstraintsB($scope.keyField), function(i, constraint) {
				r = constraint.value;
			});
			var prefix = commonPrefix(q, r);
			if (prefix) {
				var interval = Interval.match(prefix) || Interval.matchRange(prefix) || Interval.matchSymbol(prefix);
				if (interval) {
					$scope.interval = interval;
					// $scope.range = prefix; // doesn't handle OR constraints
				}
			}
			return {
				id : $scope.settings.id,
				type : 'timeline',
				key_field : $scope.settings.key_field,
				field : $scope.settings.field,
				unit : $scope.settings.unit,
				interval : $scope.interval.name,
				range : $scope.range,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.$broadcast('settings'); // notify nested widget
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.times = result[$scope.settings.id] || [];
			$scope.timesB = resultB && resultB[$scope.settings.id] || [];
			$scope.paired = isPaired($scope.times, $scope.timesB);
			$timeout($scope.draw, 1); // delay for correct width
		};
		/**
		 * Returns true if the two arrays contain an object with the same "time" properties and non-zero "count" properties.
		 */
		function isPaired(a, b) {
			for (var i = 0, j = 0; b && i < a.length && j < b.length;) {
				if (a[i].time === b[j].time) {
					if (a[i].count * b[j].count === 0) {
						++i;
						++j;
					} else {
						return true;
					}
				} else if (a[i].time < b[j].time) {
					++i;
				} else if (a[i].time > b[j].time) {
					++j;
				}
			}
			return false;
		}
		function toRanges(times) {
			var ranges = [];
			var begin = null;
			var end = null;
			var length = 0;
			$.each(times, function(i, time) {
				if (time.count > 0) {
					begin = begin || time.value;
					++length;
				} else {
					if (begin !== null) {
						ranges.push(length === 1 ? begin : '[' + begin + '..' + time.value + ')');
						begin = null;
						length = 0;
					}
				}
				end = time.value;
			});
			if (begin !== null) {
				ranges.push(length === 1 ? begin : '[' + begin + '..' + end + ']');
			}
			return ranges;
		}
		$scope.filters = {
				thisYear : function() {
					filter(moment().format('YYYY'));
				},
				lastYear : function() {
					filter(moment().subtract(1, 'years').format('YYYY'));
				},
				thisMonth : function() {
					filter(moment().format('YYYY-MM'));
				},
				lastMonth : function() {
					filter(moment().subtract(1, 'months').format('YYYY-MM'));
				},
				lastMonths : function(n) {
					filter('[' + moment().subtract(n, 'months').format('YYYY-MM') + '..' + moment().format('YYYY-MM') + ')');
				},
				thisWeek : function() {
					filter(moment().format('GGGG-[W]WW'));
				},
				lastWeek : function() {
					filter(moment().subtract(1, 'weeks').format('GGGG-[W]WW'));
				},
				today : function() {
					filter(moment().format('YYYY-MM-DD'));
				},
				yesterday : function() {
					filter(moment().subtract(1, 'days').format('YYYY-MM-DD'));
				},
				lastHours : function(n) {
					filter('[-' + n + 'h..*)');
				},
				select : function(offset) {
					var times = [];
					for (var i = 0; i < $scope.times.length; ++i) {
						if (i + offset >= 0 && i + offset < $scope.times.length) {
							times.push({
								value : $scope.times[i + offset].label,
								count : $scope.times[i].count
							});
						}
					}
					var ranges = toRanges(times);
					if (ranges.length) {
						filter(ranges.join(' OR '));
					}
				}
		};
		$scope.snapshot = function() {
			$scope.$broadcast('snapshot');
		};
		$scope.toSpreadsheet = function() {
			var header = $scope.settings.statistic + '_' + $scope.settings.field;
			if ($scope.settings.unit) {
				header += '_' + $scope.settings.unit;
			}
			var spreadsheet = new Spreadsheet([ $scope.interval.name, header ]);
			var field = Field.find($scope.settings.field);
			$.each($scope.times, function(i, time) {
				var value = time[$scope.settings.statistic || 'count'];
				spreadsheet.addRecord([ time.label, angular.isDefined(value) ? field.toNumber(value) : '' ]);
			});
			if ($scope.timesB && $scope.timesB.length) {
				spreadsheet.addHeader(header);
				$.each($scope.timesB, function(i, time) {
					var value = time[$scope.settings.statistic || 'count'];
					spreadsheet.mergeRecord([ time.label, angular.isDefined(value) ? field.toNumber(value) : '' ]);
				});
			}
			return spreadsheet;
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length || $scope.timesB && $scope.timesB.length) {
				var type = $scope.settings.statistic === 'count' || $scope.settings.statistic === 'sum' ? 'column' : 'line';
				var field = Field.find($scope.settings.field);
				var options = {
					chart : {
						animation : false,
						zoomType : 'x',
						events : {
							selection : function(event) {
								var from = null;
								var to = null;
								$.each($scope.times, function(i, time) {
									from = from || time.label;
									to = time.label;
								});
								$.each($scope.times, function(i, time) {
									if (time.time >= event.xAxis[0].min) {
										from = time.label;
										return false;
									}
								});
								$.each($scope.times, function(i, time) {
									if (time.time <= event.xAxis[0].max) {
										to = time.label;
									}
								});
								if (from !== null && to !== null) {
									var range = from === to ? from : '[' + from + '..' + to + ']';
									$scope.$apply(function() {
										filter(range);
									});
								}
								return false;
							}
						}
					},
					title : {
						text : null
					},
					xAxis : {
						type : 'datetime',
						labels : {
							overflow : 'justify'
						},
						minTickInterval : $scope.interval.minTickInterval,
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 1,
						gridLineWidth : 0
					},
					yAxis : {
						title : {
							text : null
						},
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 0,
						gridLineWidth : 0,
						startOnTick : false,
						floor : field.minValue,
						ceiling : field.maxValue
					},
					tooltip : {
						crosshairs : false,
						shared : false,
						hideDelay : 0
					},
					series : [{
						name : $scope.settings.statistic || 'count',
						type : type,
						data : [],
						color: 'rgba(47, 126, 216, 0.4)',
						lineColor: 'rgb(47, 126, 216)',
						marker : {
							symbol : 'circle',
							fillColor : 'white',
							lineWidth : 2,
							lineColor: 'rgb(47, 126, 216)'
						},
						borderRadius : 5,
						borderWidth : 2,
						zIndex: 1
					}, {
						name : 'range',
						data : [],
						type : 'arearange',
						lineWidth : 0,
						linkedTo : ':previous',
						fillColor : 'rgba(47, 126, 216, 0.1)',
						zIndex: 0
					}],
					plotOptions : {
						series : {
							animation : false,
							tooltip : {
								headerFormat : '<b>{point.key}:</b> ',
								pointFormat : "{point.tooltip}"
							}
						}
					},
					legend : {
						enabled : false
					},
					credits : {
						enabled : false
					},
					playable : true
				};
				if ($scope.interval != Interval.VALUES[Interval.VALUES.length - 1]) {
					options.plotOptions.series.cursor = 'pointer';
					options.plotOptions.series.events = {
						click : function(event) {
							$scope.$apply(function() {
								filter(event.point.options.filter);
							});
						}
					};
				}
				if ($scope.settings.placement === 'top') {
					options.chart.height = 150;
				}
				$.each($scope.times, function(i, time) {
					var value = time[$scope.settings.statistic || 'count'];
					if (value !== undefined) {
						options.series[0].data.push({ x : time.time, y : field.toNumber(value), filter : time.label, tooltip : field.toText(value) });
						if ($scope.settings.statistic === 'avg') {
							options.series[1].data.push({
								x : time.time,
								low : field.toNumber(time['min']),
								high : field.toNumber(time['max']),
								filter : time.label,
								tooltip : field.toText(time['min']) + '..' + field.toText(time['max'])
							});
						}
					} else {
						options.series[0].data.push({ x : time.time, y : null });
						if ($scope.settings.statistic === 'avg') {
							options.series[1].data.push({
								x : time.time,
								low : null,
								high : null
							});
						}
					}
				});
				if ($scope.timesB && $scope.timesB.length) {
					options.series.push({
						name : $scope.settings.statistic || 'count',
						type : type,
						data : [],
						color: 'rgba(204, 102, 0, 0.4)',
						lineColor : 'rgb(204, 102, 0)',
						marker : {
							symbol : 'circle',
							fillColor : 'white',
							lineWidth : 2,
							lineColor: 'rgb(204, 102, 0)'
						},
						borderRadius : 5,
						borderWidth : 2,
						zIndex: 1
					});
					options.series.push({
						name : 'range',
						data : [],
						type : 'arearange',
						lineWidth : 0,
						linkedTo : ':previous',
						fillColor : 'rgba(204, 102, 0, 0.1)',
						zIndex: 0
					});
					$.each($scope.timesB, function(i, time) {
						var value = time[$scope.settings.statistic || 'count'];
						if (value !== undefined) {
							options.series[2].data.push({ x : time.time, y : field.toNumber(value), filter : time.label, tooltip : field.toText(value) });
							if ($scope.settings.statistic === 'avg') {
								options.series[3].data.push([ time.time, field.toNumber(time['min']), field.toNumber(time['max']) ]);
							}
						} else {
							options.series[2].data.push({ x : time.time, y : null });
							if ($scope.settings.statistic === 'avg') {
								options.series[3].data.push([ time.time, null, null ]);
							}
						}
					});
				}
				if ($scope.times.length > 1 && $scope.settings.regression == 'linear') {
					var regression = statistics.regression(toXY($scope.times));
					if (regression) {
						options.series.push({
							type : 'line',
							data : regression.data,
							color : 'rgb(119, 152, 191)',
							dashStyle : 'Dot',
							lineWidth : 2,
							enableMouseTracking : false,
							marker : {
								enabled : false
							}
						});
					}
				}
				if ($scope.timesB && $scope.timesB.length > 1 && $scope.settings.regression == 'linear') {
					var regressionB = statistics.regression(toXY($scope.timesB));
					if (regressionB) {
						options.series.push({
							type : 'line',
							data : regressionB.data,
							color : 'rgb(204, 102, 0)',
							dashStyle : 'Dot',
							lineWidth : 2,
							enableMouseTracking : false,
							marker : {
								enabled : false
							}
						});
					}
				}
				field.formatAxis(options.yAxis);
				$scope.chartOptions = options;
			}
		};
		function toXY(times) {
			var xy = [];
			var field = Field.find($scope.settings.field);
			$.each(times, function(i, time) {
				var value = time[$scope.settings.statistic || 'count'];
				if (value !== undefined) {
					xy.push([ time.time, field.toNumber(value) ]);
				}
			});
			return xy;
		}

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('EffectSizeWidgetController', ['$scope', '$timeout', 'Field', function($scope, $timeout, Field) {

		$scope.init = function() {
			$scope.stats = null;
			$scope.statsB = null;
		};
		function shouldRequestStats() {
			return $scope.constraintsB && $scope.settings.statistic === 'avg';
		}
		$scope.params = function() {
			return shouldRequestStats() ? {
				id : $scope.settings.id + '-stats',
				type : 'stats',
				field : $scope.settings.field,
				unit : $scope.settings.unit,
				filter : $scope.settings.filter
			} : null;
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			if (shouldRequestStats()) {
				$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
					$scope.update(null, result, resultB);
				});
			} else {
				$scope.$evalAsync($scope.update);
			}
		};
		$scope.update = function(event, result, resultB) {
			if ($scope.settings.statistic === 'avg') {
				$scope.stats = result[$scope.settings.id + '-stats'];
				$scope.statsB = resultB && resultB[$scope.settings.id + '-stats'];
			} else {
				$scope.stats = stats($scope.times);
				$scope.statsB = stats($scope.timesB);
			}
			$timeout($scope.draw, 1); // delay for correct width
		};
		function stats(times) {
			var field = Field.find($scope.settings.field);
			var values = toNumbers(times, field);
			if (!values.length) {
				return null;
			}
			var r = { avg : 0, stdev : 0, count : values.length }, variance = 0;
			for (var m, s = 0, l = r.count; l--; s += values[l]);
			for (m = r.avg = s / r.count, l = r.count, s = 0; l--; s += Math.pow(values[l] - m, 2));
			r.stdev = toObject(Math.sqrt(variance = s / r.count));
			r.avg = toObject(r.avg);
			return r;
		}
		function toNumbers(items, field) {
			var numbers = $.map(items, function(item) {
				return field.toNumber(item[$scope.settings.statistic || 'count']);
			});
			return $.grep(numbers, function(number) {
				return !isNaN(number);
			});
		}
		function toObject(number) {
			return $scope.settings.unit ? { '@value' : number, 'unit' : $scope.settings.unit } : number;
		}
		$scope.draw = function() {
			if ($scope.stats && $scope.stats.avg !== undefined && $scope.statsB && $scope.statsB.avg !== undefined) {
				var field = Field.find($scope.settings.field);
				var avgA = field.toNumber($scope.stats.avg);
				var avgB = field.toNumber($scope.statsB.avg);
				var avgAB = avgB - avgA;
				var z = 1.96;
				var stdevA = field.toNumber($scope.stats.stdev);
				var stdevB = field.toNumber($scope.statsB.stdev);
				var nA = $scope.stats.count;
				var nB = $scope.statsB.count;

				var d = z * Math.sqrt((stdevA * stdevA / nA) + (stdevB * stdevB / nB));
				var lower = avgAB - d;
				var upper = avgAB + d;

				var color;
				if (lower <= 0 && upper >= 0) {
					color = '#C0C0C0'; // light-gray
				} else {
					color = '#555'; // dark-gray
				}

				var rChartOptions = {
					chart : {
						type : 'line',
						inverted : true,
						height : 75,
						plotBorderWidth : 1,
						plotBackgroundColor : '#fafafa',
						marginLeft : 45,
						marginRight : 25,
						animation : false
					},
					title : {
						text : null
					},
					xAxis : {
						title : {
							text : null
						},
						labels : {
							enabled : false
						},
						lineWidth : 0,
						tickLength : 0
					},
					yAxis : {
						title : {
							text : null
						},
						labels : {
							autoRotation : false
						},
						lineWidth : 0,
						tickColor : '#C0C0C0',
						tickWidth : 1,
						tickLength : 5,
						tickPosition : 'inside',
						gridLineWidth : 0
					},
					tooltip : {
						enabled : false
					},
					series : [{
						data : [[ 0, avgAB ]],
						color : color,
						animation : false,
						marker : {
							radius : 5,
							symbol : 'circle'
						},
						states : {
							hover : {
								enabled : false
							}
						}
					}],
					legend : {
						enabled : false
					},
					credits : {
						enabled : false
					}
				};
				if (d > 0) {
					rChartOptions.series.push({
						type : 'errorbar',
						data : [[ 0, avgAB - d, avgAB + d ]],
						lineWidth : 2,
						color : color,
						animation : false,
						states : {
							hover : {
								enabled : false
							}
						}
					});
				}
				field.formatAxis(rChartOptions.yAxis);
				$scope.rChartOptions = rChartOptions;
			}
		};

		$scope.init();
		$scope.register($scope, true);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$scope.$on('settings', function() {
			$scope.refresh();
		});
	}]);

	app.controller('TimelineWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', 'Interval', function($scope, WidgetDialogControllerSupport, Field, Interval) {

		new WidgetDialogControllerSupport($scope);

		$scope.regressionMethods = [ 'linear' ];

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0 ?
					$scope.settings.unit === null :
					$.inArray($scope.settings.unit, units) != -1;
		}
		function isStatisticValid() {
			return $.grep($scope.getStatistics($scope.settings.field), function(statistic) {
				return $scope.settings.statistic === statistic;
			}).length > 0;
		}

		$scope.init = function() {
			$scope.settings = angular.copy($scope.$parent.settings);
			$scope.settings.interval = $scope.settings.interval || Interval.VALUES[1].name;
		};
		$scope.getFields = function() {
			var fields = Field.findByType('numeric');
			fields.unshift(Field.find($scope.keyField));
			return fields;
		};
		$scope.getStatistics = function(field) {
			return field === $scope.keyField ? [ 'count' ] : [ 'sum', 'avg', 'min', 'max' ];
		};
		$scope.getUnits = function() {
			return Field.find($scope.settings.field).units || [];
		};
		$scope.subfields = $.map(Field.find($scope.keyField).subfields, function(subfield) {
			return { label : subfield, value : (subfield ? $scope.keyField + '$' + subfield : $scope.keyField) };
		});
		$scope.getIntervals = function() {
			return Interval.VALUES;
		};
		$scope.valid = function() {
			return isUnitValid() && isStatisticValid();
		};

		$scope.$watch('settings.field', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
			if (!isStatisticValid()) {
				$scope.settings.statistic = $scope.getStatistics($scope.settings.field)[0];
			}
		});
	}]);

	app.controller('PolarWidgetController', ['$scope', '$timeout', 'Field', 'Spreadsheet', function($scope, $timeout, Field, Spreadsheet) {

		$scope.keyField = 'timestamp';

		/**
		 * Based on https://stackoverflow.com/a/18070247/1144085
		 */
		function circular_avg(data) {
			var f = 2 * Math.PI / data.length; // factor for converting keys to radians
			var x = 0;
			var y = 0;
			$.each(data, function(i, time) {
				x += time.count * Math.sin(f * i);
				y += time.count * Math.cos(f * i);
			});
			var z = Math.atan2(x, y);
			if (z < 0) {
				z += 2 * Math.PI;
			}
			return (Math.round(z / f * 2) / 2) % data.length; // modulo and round to 0.5
		}

		function addPlotBand(chartOptions, value, max, color) {
			chartOptions.xAxis.plotBands.push({
				color : color,
				from: value - 0.5,
				to : value + 0.5
			});
			if (value - 0.5 < 0) {
				chartOptions.xAxis.plotBands.push({
					color : color,
					from: max - 0.5,
					to : max
				});
			}
		}

		$scope.init = function() {
			$scope.times = null;
			$scope.timesB = null;
			$scope.settings.key_field = $scope.settings.key_field || $scope.keyField;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'polar',
				key_field : $scope.settings.key_field,
				value_field : $scope.settings.value_field,
				unit : $scope.settings.unit,
				interval : $scope.settings.interval,
				filter : $scope.settings.filter
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.times = result[$scope.settings.id] || [];
			$scope.timesB = resultB && resultB[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.filter = function(value, negated) {
			$scope.addConstraint($scope.settings.key_field + '.' + $scope.settings.interval, value, true, negated);
		};
		$scope.snapshot = function() {
			$scope.$broadcast('snapshot');
		};
		$scope.toSpreadsheet = function() {
			var header = $scope.settings.statistic + '_' + $scope.settings.value_field;
			if ($scope.settings.unit) {
				header += '_' + $scope.settings.unit;
			}
			var spreadsheet = new Spreadsheet([ $scope.settings.interval, header ]);
			var field = Field.find($scope.settings.value_field);
			$.each($scope.times, function(i, time) {
				var value = time[$scope.settings.statistic || 'count'];
				spreadsheet.addRecord([ time.value, angular.isDefined(value) ? field.toNumber(value) : '' ]);
			});
			if ($scope.timesB && $scope.timesB.length) {
				spreadsheet.addHeader(header);
				$.each($scope.timesB, function(i, time) {
					var value = time[$scope.settings.statistic || 'count'];
					spreadsheet.mergeRecord([ time.value, angular.isDefined(value) ? field.toNumber(value) : '' ]);
				});
			}
			return spreadsheet;
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length || $scope.timesB && $scope.timesB.length) {
				var field = Field.find($scope.settings.value_field);
				var options = {
					chart : {
						type : 'column',
						polar: true,
						animation : false
					},
					title : {
						text : null
					},
					xAxis : {
						categories : [],
						plotBands: []
					},
					yAxis : {
						title : {
							text : null
						},
						floor : field.minValue,
						ceiling : field.maxValue
					},
					tooltip : {
						shared : false,
						hideDelay : 0,
						formatter : function() {
							return '<b>' + this.x + '</b>: ' + (field.toText(this.y) || this.y) + ($scope.settings.unit || '');
						}
					},
					series : [{
						name : $scope.settings.statistic || 'count',
						data : []
					}],
					plotOptions : {
						series : {
							color : 'rgba(47, 126, 216, 0.4)',
							animation : false,
							pointPlacement: 'on',
							cursor : 'pointer',
							events : {
								click : function(event) {
									$scope.$apply(function() {
										$scope.filter($scope.times[event.point.x].value);
									});
								}
							}
						},
						column : {
							pointPadding: 0,
							groupPadding: 0
						}
					},
					legend : {
						enabled : false
					},
					credits: {
						enabled: false
					}
				};
				if ($scope.settings.mark === 'avg') {
					addPlotBand(options, circular_avg($scope.times), $scope.times.length, 'rgba(47, 126, 216, 0.2)');
				}
				if ($scope.settings.placement === 'top') {
					options.chart.height = 150;
				}
				$.each($scope.times, function(i, time) {
					var value = time[$scope.settings.statistic || 'count'];
					options.xAxis.categories.push(time.label);
					options.series[0].data.push(value !== undefined ? field.toNumber(value) : 0);
				});
				if ($scope.timesB && $scope.timesB.length) {
					options.series.push({
						name : $scope.settings.statistic || 'count',
						data : [],
						color: 'rgba(204, 102, 0, 0.4)',
						events : {
							click : function(event) {
								$scope.$apply(function() {
									$scope.filter($scope.timesB[event.point.x].value);
								});
							}
						}
					});
					$.each($scope.timesB, function(i, time) {
						var value = time[$scope.settings.statistic || 'count'];
						options.xAxis.categories.push(time.label);
						options.series[1].data.push(value !== undefined ? field.toNumber(value) : 0);
					});
					if ($scope.settings.mark === 'avg') {
						addPlotBand(options, circular_avg($scope.timesB), $scope.timesB.length, 'rgba(204, 102, 0, 0.2)');
					}
				}
				field.formatAxis(options.yAxis);
				$scope.chartOptions = options;
			}
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('PolarWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope);

		$scope.intervals = [
			{ id : 'hour_of_day', label : 'hour of day' },
			{ id : 'day_of_week', label : 'day of week' },
			{ id : 'day_of_month', label : 'day of month' },
			{ id : 'month_of_year', label : 'month of year' }
		];

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0 ?
				$scope.settings.unit === null :
				$.inArray($scope.settings.unit, units) != -1;
		}
		function isStatisticValid() {
			return $.grep($scope.getStatistics($scope.settings.value_field), function(statistic) {
				return $scope.settings.statistic === statistic;
			}).length > 0;
		}

		$scope.subfields = $.map(Field.find($scope.keyField).subfields, function(subfield) {
			return { label : subfield, value : (subfield ? $scope.keyField + '$' + subfield : $scope.keyField) };
		});
		$scope.getFields = function() {
			var fields = Field.findByType('numeric');
			fields.unshift(Field.find($scope.keyField));
			return fields;
		};
		$scope.getStatistics = function(field) {
			return field === $scope.keyField ? [ 'count' ] : [ 'sum', 'avg', 'min', 'max' ];
		};
		$scope.getUnits = function() {
			var valueField = Field.find($scope.settings.value_field);
			return valueField ? valueField.units : [];
		};
		$scope.getIntervals = function() {
			return Interval.VALUES;
		};
		$scope.valid = function() {
			return isUnitValid() && isStatisticValid();
		};

		$scope.$watch('settings.value_field', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
			if (!isStatisticValid()) {
				$scope.settings.statistic = $scope.getStatistics($scope.settings.value_field)[0];
			}
		});
	}]);


	/**
	 * Based on https://github.com/virtualstaticvoid/highcharts_trendline
	 */
	app.factory('statistics', function() {

		function regression(X, Y) {
			var N = X.length;
			var SX = 0;
			var SY = 0;
			var SXX = 0;
			var SXY = 0;
			var SYY = 0;
			for (var i = 0; i < N; ++i) {
				SX = SX + X[i];
				SY = SY + Y[i];
				SXY = SXY + X[i] * Y[i];
				SXX = SXX + X[i] * X[i];
				SYY = SYY + Y[i] * Y[i];
			}
			var slope = (N * SXY - SX * SY) / (N * SXX - SX * SX);
			var intercept = (SY - slope * SX) / N;
			return {
				slope : slope,
				intercept : intercept
			};
		}

		function pearson(x, y) {

			console.assert(x.length == y.length, 'expected arrays with same length');

			var n = x.length;
			var xy = [];
			var x2 = [];
			var y2 = [];

			for (var i = 0; i < n; ++i) {
				xy.push(x[i] * y[i]);
				x2.push(x[i] * x[i]);
				y2.push(y[i] * y[i]);
			}

			var sum_x = 0;
			var sum_y = 0;
			var sum_xy = 0;
			var sum_x2 = 0;
			var sum_y2 = 0;

			for (i = 0; i < n; ++i) {
				sum_x += x[i];
				sum_y += y[i];
				sum_xy += xy[i];
				sum_x2 += x2[i];
				sum_y2 += y2[i];
			}

			var step1 = (n * sum_xy) - (sum_x * sum_y);
			var step2 = (n * sum_x2) - (sum_x * sum_x);
			var step3 = (n * sum_y2) - (sum_y * sum_y);
			var step4 = Math.sqrt(step2 * step3);
			var answer = step1 / step4;

			return answer;
		}

		function rank(x) {
			var ranked = [];
			$.each(x, function(i, a) {
				var rank = 1;
				var freq = 0;
				$.each(x, function(j, b) {
					if (b > a) {
						++rank;
					} else if (b == a) {
						++freq;
					}
				});
				if (freq > 1) {
					rank = (freq * (2 * rank + freq - 1)) / (2 * freq); // derived from sum of arithmetic sequence formula
				}
				ranked.push(rank);
			});
			return ranked;
		}

		function tanh(x) {
			var e = Math.exp(2 * x);
			return (e - 1) / (e + 1);
		}

		function atanh(x) {
			return 0.5 * (log1p(x) - log1p(-x));
		}

		/**
		 * Computes log(1 + x) accurately for small values of x.
		 * Based on http://phpjs.org/functions/log1p/.
		 */
		function log1p(x) {
			if (x <= -1) {
				return Number.NEGATIVE_INFINITY;
			}
			if (x < 0 || x > 1) {
				return Math.log(1 + x);
			}
			var value = 0;
			var precision = 50;
			for (var i = 1; i < precision; ++i) {
				if ((i % 2) === 0) {
					value -= Math.pow(x, i) / i;
				} else {
					value += Math.pow(x, i) / i;
				}
			}
			return value;
		}

		/**
		 * Computes the 95% confidence interval for a correlation coefficient.
		 * Based on https://stats.stackexchange.com/a/18904.
		 */
		function confidence(r, n) {
			console.assert(n > 3, 'not enough samples');
			var stderr = 1.0 / Math.sqrt(n - 3);
			var delta = 1.96 * stderr;
			var lower = tanh(atanh(r) - delta);
			var upper = tanh(atanh(r) + delta);
			return [ lower, upper ];
		}

		return {
			regression : function(data) {
				var x = [];
				var y = [];
				var min = 0;
				var max = 0;
				var i = 0;
				for (; i < data.length; ++i) {
					x.push(data[i][0]);
					y.push(data[i][1]);
					if (data[i][0] > data[max][0]) {
						max = i;
					}
					if (data[i][0] < data[min][0]) {
						min = i;
					}
				}
				var params = regression(x, y);
				return !isNaN(params.slope) ? {
					data : [
						[x[min], params.slope * x[min] + params.intercept],
						[x[max], params.slope * x[max] + params.intercept]
					],
					slope : params.slope,
					intercept : params.intercept
				} : null;
			},
			correlate : function(data, ranked) {
				var x = [];
				var y = [];
				var i = 0;
				for (; i < data.length; ++i) {
					x.push(data[i][0]);
					y.push(data[i][1]);
				}
				if (ranked) {
					x = rank(x);
					y = rank(y);
				}
				var r = pearson(x, y);
				var c = confidence(r, x.length);
				return {
					r : r,
					lower : c[0],
					upper : c[1]
				};
			}
		};
	});

	app.controller('ScatterPlotWidgetController', ['$scope', '$timeout', 'Field', 'Spreadsheet', 'timezone', 'statistics', function($scope, $timeout, Field, Spreadsheet, timezone, statistics) {

		$scope.keyField = 'timestamp';

		$scope.init = function() {
			$scope.data = null;
			$scope.settings.key_field = $scope.settings.key_field || $scope.keyField;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'scatterplot',
				field_x : $scope.settings.field_x,
				unit_x : $scope.settings.unit_x,
				statistic_x : $scope.settings.statistic_x,
				filter_x : $scope.settings.filter_x,
				field_y : $scope.settings.field_y,
				unit_y : $scope.settings.unit_y,
				statistic_y : $scope.settings.statistic_y,
				filter_y : $scope.settings.filter_y,
				key_field : $scope.settings.key_field,
				interval : $scope.settings.interval,
				lag : $scope.settings.lag
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			var params = $.extend($scope.params(), options, settings);
			if (/hour|minute|second/.test(params.interval)) {
				params.timezone = timezone;
			}
			$scope.search([ params ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.data = result[$scope.settings.id] || [];
			$scope.dataB = resultB && resultB[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.snapshot = function() {
			$scope.$broadcast('snapshot');
		};
		$scope.toSpreadsheet = function() {
			function buildHeader(label, statistic, field, unit) {
				var header = label;
				if (!header) {
					header = statistic + '_' + field;
					if (unit) {
						header += '_' + unit;
					}
				}
				return header;
			}
			var compareMode = $scope.dataB && $scope.dataB.length;
			var spreadsheet = new Spreadsheet([
				buildHeader($scope.settings.label_x, $scope.settings.statistic_x, $scope.settings.field_x, $scope.settings.unit_x),
				buildHeader($scope.settings.label_y, $scope.settings.statistic_y, $scope.settings.field_y, $scope.settings.unit_y),
				compareMode ? 'dataset' : ''
			]);
			$.each($scope.data, function(i, value) {
				spreadsheet.addRecord([ value[0], value[1], compareMode ? 'a' : '' ]);
			});
			if (compareMode) {
				$.each($scope.dataB, function(i, value) {
					spreadsheet.addRecord([ value[0], value[1], 'b' ]);
				});
			}
			return spreadsheet;
		};
		$scope.draw = function() {
			function buildLabel(label, statistic, field, unit) {
				var header = label;
				if (!header) {
					header = statistic + ' of ' + field;
					if (unit) {
						header += ' (' + unit + ')';
					}
				}
				return header;
			}
			var xField = Field.find($scope.settings.field_x);
			var yField = Field.find($scope.settings.field_y);
			if ($scope.data && $scope.data.length || $scope.dataB && $scope.dataB.length) {
				var options = {
					chart : {
						type : 'scatter',
						zoomType: 'xy',
						animation : false
					},
					title : {
						text : null
					},
					xAxis : {
						title : {
							text : buildLabel($scope.settings.label_x, $scope.settings.statistic_x, $scope.settings.field_x, $scope.settings.unit_x)
						},
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 0,
						gridLineWidth : 0,
						startOnTick : false,
						floor : xField.minValue,
						ceiling : xField.maxValue
					},
					yAxis : {
						title : {
							text : buildLabel($scope.settings.label_y, $scope.settings.statistic_y, $scope.settings.field_y, $scope.settings.unit_y)
						},
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 0,
						gridLineWidth : 0,
						startOnTick : false,
						floor : yField.minValue,
						ceiling : yField.maxValue
					},
					tooltip : {
						crosshairs : false,
						shared : false,
						hideDelay : 0,
						formatter : function() {
							return '<b>x</b>: ' + (xField.toText(this.x) || this.x) + ($scope.settings.unit_x || '') + ', ' +
								'<b>y</b>: ' + (yField.toText(this.y) || this.y) + ($scope.settings.unit_y || '');
						}
					},
					series : [{
						data : $scope.data,
						animation : false,
						color : 'rgba(119, 152, 191, 0.5)',
						allowPointSelect : true,
						marker : {
							radius : 5,
							symbol : 'circle'
						}
					}],
					plotOptions : {
						series : {
							animation : false,
							stickyTracking : false
						}
					},
					legend: {
						enabled: false
					},
					credits: {
						enabled: false
					}
				};
				if ($scope.dataB && $scope.dataB.length) {
					options.series.push({
						data : $scope.dataB,
						animation : false,
						color : 'rgba(204, 102, 0, 0.5)',
						allowPointSelect : true,
						marker : {
							radius : 5,
							symbol : 'circle'
						}
					});
				}
				if ($scope.data.length > 1 && $scope.settings.regression == 'linear') {
					var regression = statistics.regression($scope.data);
					if (regression) {
						options.series.push({
							type : 'line',
							data : regression.data,
							color : 'rgb(119, 152, 191)',
							dashStyle : 'Dot',
							lineWidth : 2,
							enableMouseTracking : false,
							marker : {
								enabled : false
							}
						});
					}
				}
				if ($scope.dataB && $scope.dataB.length > 1 && $scope.settings.regression == 'linear') {
					var regressionB = statistics.regression($scope.dataB);
					if (regressionB) {
						options.series.push({
							type : 'line',
							data : regressionB.data,
							color : 'rgb(204, 102, 0)',
							dashStyle : 'Dot',
							lineWidth : 2,
							enableMouseTracking : false,
							marker : {
								enabled : false
							}
						});
					}
				}
				if ($scope.data.length > 3 || $scope.dataB.length > 3) {
					var rChartOptions = {
						chart : {
							type : 'line',
							inverted : true,
							height : 75,
							plotBorderWidth : 1,
							plotBackgroundColor : '#fafafa',
							marginLeft : 65,
							animation : false
						},
						title : {
							text : null
						},
						xAxis : {
							title : {
								text : null
							},
							labels : {
								enabled : false
							},
							lineWidth : 0,
							tickLength : 0
						},
						yAxis : {
							title : {
								text : null
							},
							max : 1.0,
							min : -1.0,
							lineWidth : 0,
							tickInterval : 1.0,
							tickWidth : 0,
							gridLineWidth : 1
						},
						tooltip : {
							shared : true,
							hideDelay : 0
						},
						series : [],
						legend : {
							enabled : false
						},
						credits : {
							enabled : false
						}
					};
					if ($scope.data && $scope.data.length > 3) {
						var correlation = statistics.correlate($scope.data, true);
						rChartOptions.series.push({
							data : [[ 0, correlation.r ]],
							color : 'rgb(119, 152, 191)',
							animation : false,
							marker : {
								radius : 5,
								symbol : 'circle'
							},
							tooltip : {
								headerFormat : '',
								pointFormat : "<b>Spearman's rho:</b> {point.y}<br/>",
								valueDecimals : 3
							},
							states : {
								hover : {
									enabled : false
								}
							}
						});
						rChartOptions.series.push({
							type : 'errorbar',
							data : [[ 0, correlation.lower, correlation.upper ]],
							lineWidth : 2,
							color : 'rgb(119, 152, 191)',
							animation : false,
							tooltip : {
								headerFormat : '',
								pointFormat : '<b>95% confidence interval:</b> [' + correlation.lower.toFixed(3) + '..' + correlation.upper.toFixed(3) + ']<br/>'
							}
						});
					}
					if ($scope.dataB && $scope.dataB.length > 3) {
						var correlationB = statistics.correlate($scope.dataB, true);
						rChartOptions.series.push({
							data : [[ 1, correlationB.r ]],
							color : 'rgb(204, 102, 0)',
							animation : false,
							marker : {
								radius : 5,
								symbol : 'circle'
							},
							tooltip : {
								headerFormat : '',
								pointFormat : "<b>Spearman's rho:</b> {point.y}<br/>",
								valueDecimals : 3
							},
							states : {
								hover : {
									enabled : false
								}
							}
						});
						rChartOptions.series.push({
							type : 'errorbar',
							data : [[ 1, correlationB.lower, correlationB.upper ]],
							lineWidth : 2,
							color : 'rgb(204, 102, 0)',
							animation : false,
							tooltip : {
								headerFormat : '',
								pointFormat : '<b>95% confidence interval:</b> [' + correlationB.lower.toFixed(3) + '..' + correlationB.upper.toFixed(3) + ']<br/>'
							}
						});
					}
					$scope.rChartOptions = rChartOptions;
				}
				if ($scope.settings.placement === 'top') {
					options.chart.height = 150;
				}
				xField.formatAxis(options.xAxis);
				yField.formatAxis(options.yAxis);
				$scope.chartOptions = options;
			}
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('ScatterPlotWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', 'Interval', function($scope, WidgetDialogControllerSupport, Field, Interval) {

		new WidgetDialogControllerSupport($scope);

		var keyField = Field.find('timestamp');

		function isUnitValid(field, unit) {
			var units = $scope.getUnits(field);
			return units.length === 0 ?
				unit === null :
				$.inArray(unit, units) != -1;
		}

		$scope.regressionMethods = [ 'linear' ];

		$scope.getFields = function() {
			var fields = Field.findByType('numeric');
			fields.unshift(keyField);
			return fields;
		};
		$scope.subfields = $.map(Field.find($scope.keyField).subfields, function(subfield) {
			return { label : subfield, value : (subfield ? $scope.keyField + '$' + subfield : $scope.keyField) };
		});
		$scope.getIntervals = function() {
			return Interval.VALUES;
		};
		$scope.getStatistics = function(field) {
			return field === keyField.name ? [ 'count' ] : [ 'sum', 'avg', 'min', 'max' ];
		};
		$scope.getUnits = function(field) {
			return field && Field.find(field).units || [];
		};
		$scope.valid = function() {
			return isUnitValid($scope.settings.field_x, $scope.settings.unit_x) &&
				isUnitValid($scope.settings.field_y, $scope.settings.unit_y);
		};
		$scope.swap = function() {
			function swap(object, p1, p2) {
				var tmp = object[p1];
				object[p1] = object[p2];
				object[p2] = tmp;
			}
			swap($scope.settings, 'label_x', 'label_y');
			swap($scope.settings, 'statistic_x', 'statistic_y');
			swap($scope.settings, 'field_x', 'field_y');
			swap($scope.settings, 'unit_x', 'unit_y');
			swap($scope.settings, 'filter_x', 'filter_y');
			if ($scope.settings.lag) {
				$scope.settings.lag = -$scope.settings.lag;
			}
		};

		$scope.$watch('settings.field_x', function() {
			if (!isUnitValid($scope.settings.field_x, $scope.settings.unit_x)) {
				$scope.settings.unit_x = null;
			}
		});
		$scope.$watch('settings.field_y', function() {
			if (!isUnitValid($scope.settings.field_y, $scope.settings.unit_y)) {
				$scope.settings.unit_y = null;
			}
		});
	}]);

	app.controller('MapWidgetController', ['$scope', '$timeout', function($scope, $timeout) {

		$scope.field = 'location';

		$scope.init = function() {
			$scope.map = null;
			$scope.points = null;
			$scope.pointsB = null;
			$scope.bounds = null;
			$scope.boundsB = null;
			$scope.markers = [];
			$scope.factor = 1.0;
			$scope.shown = false;
		};
		function params(settings) {
			if (settings) {
				var filters = [];
				if (settings.filter) {
					filters.push(settings.filter);
				}
				return {
					id : settings.id,
					type : 'geobounds',
					filter : filters.join('|')
				};
			}
		}
		$scope.params = function() {
			return params($scope.settings);
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend(params($scope.settings), params(settings)) ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.bounds = toBounds(result[$scope.settings.id] || {});
			$scope.boundsB = toBounds(resultB && resultB[$scope.settings.id] || {});
			$timeout($scope.draw, 1); // delay for correct width
		};
		function toBounds(result) {
			var bounds;
			if (angular.isDefined(result.lat_min)) {
				var sw = new google.maps.LatLng(result.lat_min, result.lon_min);
				var ne = new google.maps.LatLng(result.lat_max, result.lon_max);
				bounds = new google.maps.LatLngBounds(sw, ne);
			} else {
				bounds = new google.maps.LatLngBounds();
			}
			return bounds;
		}
		var boundsUpdate;
		$scope.draw = function() {
			if (!$scope.bounds.isEmpty() || !$scope.boundsB.isEmpty()) {
				var options = {
					mapTypeId : google.maps.MapTypeId.ROADMAP,
					streetViewControl : false,
					mapTypeControlOptions : {
						style : google.maps.MapTypeControlStyle.DROPDOWN_MENU
					},
					styles : [ { 'stylers' : [ { 'saturation' : -100 } ] } ],
					minZoom : 1
				};
				$scope.map = new google.maps.Map(document.getElementById($scope.settings.id + '-map'), options);
				$scope.map.fitBounds($scope.bounds.union($scope.boundsB));
				google.maps.event.addListener($scope.map, 'bounds_changed', function() {
					$timeout.cancel(boundsUpdate);
					boundsUpdate = $timeout(function() {
						var bounds = $scope.map.getBounds();
						if (bounds.toSpan().lat() !== 0) {
							if ($scope.map.getZoom() <= 4) {
								$scope.factor = 1.0;
							} else if ($scope.map.getZoom() <= 7) {
								$scope.factor = 0.8;
							} else if ($scope.map.getZoom() <= 9) {
								$scope.factor = 0.6;
							} else if ($scope.map.getZoom() <= 12) {
								$scope.factor = 0.4;
							} else if ($scope.map.getZoom() <= 14) {
								$scope.factor = 0.2;
							} else {
								$scope.factor = 0.0;
							}
							$scope.bounds = bounds;
						}
					}, 1000);
				});
				$scope.shown = false;
				drawConstraintBounds($scope.getConstraints($scope.field), 'rgb(47, 126, 216)');
				drawConstraintBounds($scope.getConstraintsB($scope.field), 'rgb(204, 102, 0)');
				$scope.map.controls[google.maps.ControlPosition.TOP_RIGHT].push($scope.createFilterControl());
			} else {
				$('#' + $scope.settings.id + 'map').html('<i class="none">None</i>');
			}
		};
		function drawConstraintBounds(constraints, lineColor) {
			$.each(constraints, function(i, constraint) {
				var c = constraint.value.split(',');
				if (c.length === 4) {
					var sw = new google.maps.LatLng(c[0], c[1]);
					var ne = new google.maps.LatLng(c[2], c[3]);
					new google.maps.Rectangle({
						strokeColor : lineColor,
						strokeOpacity : 0.8,
						strokeWeight : 2,
						fillOpacity : 0,
						map : $scope.map,
						bounds : new google.maps.LatLngBounds(sw, ne),
						clickable : false
					});
				}
			});
		}
		$scope.createFilterControl = function() {
			var parent = document.createElement('div');
			parent.style.padding = '5px';
			var control = document.createElement('div');
			control.title = 'Click to filter using the current map bounds';
			control.className = 'map-control';
			parent.appendChild(control);
			var label = document.createElement('div');
			label.innerHTML = 'Filter';
			control.appendChild(label);
			control.addEventListener('click', function() {
				$scope.$apply(function() {
					$scope.filterBounds();
				});
			});
			return parent;
		};
		$scope.pointsParams = function() {
			return {
				id : $scope.settings.id,
				type : 'map',
				field : 'location',
				factor : $scope.factor,
				filter : getFilter()
			};
		};
		function getFilter() {
			var filter = $scope.settings.filter;
			if ($scope.bounds && !$scope.bounds.isEmpty()) {
				filter = filter ? filter + '|' : '';
				filter += 'location:' + [
					$scope.bounds.getSouthWest().lat(),
					$scope.bounds.getSouthWest().lng(),
					$scope.bounds.getNorthEast().lat(),
					$scope.bounds.getNorthEast().lng()
				].join(',');
			}
			return filter;
		}
		$scope.refreshPoints = function() {
			$scope.search([ $scope.pointsParams() ], function(result, resultB) {
				$scope.updatePoints(null, result, resultB);
			});
		};
		$scope.updatePoints = function(event, result, resultB) {
			$scope.points = result[$scope.settings.id] || [];
			$scope.pointsB = resultB && resultB[$scope.settings.id] || [];
			$scope.addPoints();
		};
		$scope.filterBounds = function() {
			$scope.addConstraint($scope.field, $scope.map.getBounds().toUrlValue(3), true);
		};
		$scope.addPoints = function() {
			$.each($scope.markers, function(i, marker) {
				marker.setMap(null);
			});
			$scope.markers = [];
			if ($scope.map && ($scope.points && $scope.points.length || $scope.pointsB && $scope.pointsB.length)) {
				$.each($scope.points, function(i, point) {
					var marker = new google.maps.Marker({
						position : new google.maps.LatLng(point.lat, point.lon),
						map : $scope.map,
						title : point.count + (point.count == 1 ? ' event' : ' events'),
						icon : {
							path : google.maps.SymbolPath.CIRCLE,
							fillOpacity : 1 - 1 / (point.count + 1), // [0.5..1.0]
							fillColor : 'rgb(47, 126, 216)',
							strokeWeight : 0,
							scale : 5
						}
					});
					$scope.markers.push(marker);
					var sw = new google.maps.LatLng(point.lat_min, point.lon_min);
					var ne = new google.maps.LatLng(point.lat_max, point.lon_max);
					var filterBounds = new google.maps.LatLngBounds(sw, ne);
					if (point.lat_min != point.lat_max) {
						var options = {
							bounds : filterBounds,
							strokeWeight : 0,
							fillOpacity : 0,
							clickable : true,
							visible : true,
							map : $scope.map
						};
						var filterRectangle = new google.maps.Rectangle(options);
						google.maps.event.addListener(filterRectangle, 'mouseover', function() {
							options.strokeWeight = 1;
							filterRectangle.setOptions(options);
						});
						google.maps.event.addListener(filterRectangle, 'mouseout', function() {
							options.strokeWeight = 0;
							filterRectangle.setOptions(options);
						});
						google.maps.event.addListener(filterRectangle, 'click', function() {
							$scope.$apply(function() {
								$scope.addConstraint($scope.field, filterBounds.toUrlValue(6), true);
							});
						});
						google.maps.event.addListener(marker, 'click', function() {
							$scope.$apply(function() {
								$scope.addConstraint($scope.field, filterBounds.toUrlValue(6), true);
							});
						});
						$scope.markers.push(filterRectangle);
					}
				});
				$.each($scope.pointsB, function(i, point) {
					$scope.markers.push(new google.maps.Marker({
						position : new google.maps.LatLng(point.lat, point.lon),
						map : $scope.map,
						title : point.count + (point.count == 1 ? ' event' : ' events'),
						icon : {
							path : google.maps.SymbolPath.CIRCLE,
							fillOpacity : 1 - 1 / (point.count + 1), // [0.5..1.0]
							fillColor : 'rgb(204, 102, 0)',
							strokeWeight : 0,
							scale : 5
						}
					}));
				});
			}
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$scope.$watch('bounds', function() {
			if ($scope.map) {
				$scope.refreshPoints();
			}
		}, true);
		$('#' + $scope.settings.id + '-tab').on('shown', function() {
			if ($scope.map && !$scope.shown) {
				google.maps.event.trigger($scope.map, 'resize');
				$scope.map.fitBounds($scope.bounds);
				$scope.shown = true;
			}
		});
	}]);

	app.controller('MapWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', function($scope, WidgetDialogControllerSupport) {

		new WidgetDialogControllerSupport($scope);
	}]);

	app.controller('HeatmapWidgetController', ['$scope', '$timeout', 'Field', function($scope, $timeout, Field) {

		$scope.field = 'location';

		$scope.init = function() {
			$scope.map = null;
			$scope.points = null;
			$scope.pointsB = null;
			$scope.heatmap = null;
			$scope.heatmapB = null;
			$scope.bounds = null;
			$scope.boundsB = null;
			$scope.precision = 8;
			$scope.shown = false;
		};
		function params(settings) {
			if (settings) {
				var filters = [];
				if (settings.filter) {
					filters.push(settings.filter);
				}
				if (settings.value_field) {
					filters.push(settings.value_field + ':*');
				}
				return {
					id : settings.id,
					type : 'geobounds',
					filter : filters.join('|')
				};
			}
		}
		$scope.params = function() {
			return params($scope.settings);
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend(params($scope.settings), params(settings)) ], function(result, resultB) {
				$.extend($scope, options);
				$.extend($scope.settings, settings);
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.bounds = toBounds(result[$scope.settings.id] || {});
			$scope.boundsB = toBounds(resultB && resultB[$scope.settings.id] || {});
			$timeout($scope.draw, 1); // delay for correct width
		};
		function toBounds(result) {
			var bounds;
			if (angular.isDefined(result.lat_min)) {
				var sw = new google.maps.LatLng(result.lat_min, result.lon_min);
				var ne = new google.maps.LatLng(result.lat_max, result.lon_max);
				bounds = new google.maps.LatLngBounds(sw, ne);
			} else {
				bounds = new google.maps.LatLngBounds();
			}
			return bounds;
		}
		var boundsUpdate;
		$scope.draw = function() {
			if (!$scope.bounds.isEmpty() || !$scope.boundsB.isEmpty()) {
				var options = {
					mapTypeId: google.maps.MapTypeId.ROADMAP,
					streetViewControl: false,
					mapTypeControlOptions : {
						style : google.maps.MapTypeControlStyle.DROPDOWN_MENU
					},
					styles : [ { 'stylers' : [ { 'saturation' : -100 } ] } ],
					minZoom : 1
				};
				$scope.map = new google.maps.Map(document.getElementById($scope.settings.id + '-map'), options);
				$scope.map.fitBounds($scope.bounds.union($scope.boundsB));
				google.maps.event.addListener($scope.map, 'bounds_changed', function() {
					$timeout.cancel(boundsUpdate);
					boundsUpdate = $timeout(function() {
						var bounds = $scope.map.getBounds();
						if (bounds.toSpan().lat() !== 0) {
							$scope.precision = Math.min(Math.ceil($scope.map.getZoom() / 3.0) + 3, 9);
							$scope.bounds = bounds;
						}
					}, 1000);
				});
				$scope.shown = false;

				var gradient = [ 'rgba(0, 126, 216, 0.0)' ];
				for (var r = 0; r < 256; ++r) {
					gradient.push('rgba(' + r + ', 126, 216, 1.0)');
				}
				$scope.heatmap = new google.maps.visualization.HeatmapLayer({
					data : [],
					map : $scope.map,
					opacity : 0.5,
					dissipating : true,
					radius : 20,
					gradient : gradient
				});
				var gradientB = [ 'rgba(204, 102, 0, 0.0)' ];
				for (var g = 103; g < 256; ++g) {
					gradientB.push('rgba(204, ' + g + ', 0, 1.0)');
				}
				$scope.heatmapB = new google.maps.visualization.HeatmapLayer({
					data : [],
					map : $scope.map,
					opacity : 0.5,
					dissipating : true,
					radius : 20,
					gradient : gradientB
				});
				drawConstraintBounds($scope.getConstraints($scope.field), 'rgb(47, 126, 216)');
				drawConstraintBounds($scope.getConstraintsB($scope.field), 'rgb(204, 102, 0)');
				$scope.map.controls[google.maps.ControlPosition.TOP_RIGHT].push(createFilterControl());
			} else {
				$('#' + $scope.settings.id + 'map').html('<i class="none">None</i>');
			}
		};
		function drawConstraintBounds(constraints, lineColor) {
			$.each(constraints, function(i, constraint) {
				var c = constraint.value.split(',');
				if (c.length === 4) {
					var sw = new google.maps.LatLng(c[0], c[1]);
					var ne = new google.maps.LatLng(c[2], c[3]);
					new google.maps.Rectangle({
						strokeColor : lineColor,
						strokeOpacity : 0.8,
						strokeWeight : 2,
						fillOpacity : 0,
						map : $scope.map,
						bounds : new google.maps.LatLngBounds(sw, ne),
						clickable : false
					});
				}
			});
		}
		function createFilterControl() {
			var parent = document.createElement('div');
			parent.style.padding = '5px';
			var control = document.createElement('div');
			control.title = 'Click to filter using the current map bounds';
			control.className = 'map-control';
			parent.appendChild(control);
			var label = document.createElement('div');
			label.innerHTML = 'Filter';
			control.appendChild(label);
			control.addEventListener('click', function() {
				$scope.$apply(function() {
					$scope.filterBounds();
				});
			});
			return parent;
		}
		$scope.pointsParams = function() {
			return {
				id : $scope.settings.id,
				type : 'heatmap',
				precision : $scope.precision,
				value_field : $scope.settings.value_field,
				unit : $scope.settings.unit,
				filter : getFilter()
			};
		};
		function getFilter() {
			var filter = $scope.settings.filter;
			if ($scope.bounds && !$scope.bounds.isEmpty()) {
				filter = filter ? filter + '|' : '';
				filter += 'location:' + [
					$scope.bounds.getSouthWest().lat(),
					$scope.bounds.getSouthWest().lng(),
					$scope.bounds.getNorthEast().lat(),
					$scope.bounds.getNorthEast().lng()
				].join(',');
			}
			return filter;
		}
		$scope.refreshPoints = function() {
			$scope.search([ $scope.pointsParams() ], function(result, resultB) {
				$scope.updatePoints(null, result, resultB);
			});
		};
		$scope.updatePoints = function(event, result, resultB) {
			$scope.points = result[$scope.settings.id] || [];
			$scope.pointsB = resultB && resultB[$scope.settings.id] || [];
			$scope.addPoints();
		};
		$scope.filterBounds = function() {
			$scope.addConstraint($scope.field, $scope.map.getBounds().toUrlValue(3), true);
		};
		$scope.addPoints = function() {
			if ($scope.map && ($scope.points && $scope.points.length || $scope.pointsB && $scope.pointsB.length)) {
				var data = [];
				var field = Field.find($scope.settings.value_field);
				$.each($scope.points, function(i, point) {
					var latLng = new google.maps.LatLng(point.lat, point.lon);
					var weight = field && $scope.points.length > 1 ? field.toNumber(point.sum) : point.count;
					data.push({ location : latLng, weight : weight });
				});
				$scope.heatmap.setData(data);
				var dataB = [];
				$.each($scope.pointsB, function(i, point) {
					var latLng = new google.maps.LatLng(point.lat, point.lon);
					var weight = field && $scope.pointsB.length > 1 ? field.toNumber(point.sum) : point.count;
					dataB.push({ location : latLng, weight : weight });
				});
				$scope.heatmapB.setData(dataB);
			}
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$scope.$watch('bounds', function() {
			if ($scope.map) {
				$scope.refreshPoints();
			}
		}, true);
		$('#' + $scope.settings.id + '-tab').on('shown', function() {
			if ($scope.map && !$scope.shown) {
				google.maps.event.trigger($scope.map, 'resize');
				$scope.map.fitBounds($scope.bounds);
				$scope.shown = true;
			}
		});
	}]);

	app.controller('HeatmapWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		new WidgetDialogControllerSupport($scope);

		$scope.getFields = function() {
			return Field.findByType('numeric');
		};

		$scope.$watch('settings.value_field', function(fieldName) {
			var field = Field.find(fieldName);
			$scope.settings.unit = field && field.units.length ? field.units[0] : '';
		});

	}]);

	app.controller('SonificationWidgetController', ['$scope', '$window', '$interval', function($scope, $window, $interval) {

		var audio = null;

		var pitches = {
			'A3' : 220.0,
			'Bb3' : 233.1,
			'B3' : 246.9,
			'C4' : 261.6,
			'C#4' : 277.2,
			'D4' : 293.7,
			'Eb4' : 311.1,
			'E4' : 329.6,
			'F4' : 349.2,
			'F#4' : 370.0,
			'G4' : 392.0,
			'G#4' : 415.3,
			'A4' : 440.0,
			'Bb4' : 466.2,
			'B4' : 493.9,
			'C5' : 523.3,
			'Cs5' : 554.4,
			'D5' : 587.3,
			'Eb5' : 622.3,
			'E5' : 659.3,
			'F5' : 698.5,
			'F#5' : 740.0
		};

		var scales = {
			chromatic : [
				pitches['C4'],
				pitches['C#4'],
				pitches['D4'],
				pitches['Eb4'],
				pitches['E4'],
				pitches['F4'],
				pitches['F#4'],
				pitches['G4'],
				pitches['G#4'],
				pitches['A4'],
				pitches['Bb4'],
				pitches['B4'],
				pitches['C5']
			],
			octatonic : [
				pitches['C4'],
				pitches['C#4'],
				pitches['Eb4'],
				pitches['E4'],
				pitches['F#4'],
				pitches['G4'],
				pitches['A4'],
				pitches['Bb4'],
				pitches['C5']
			],
			pentatonic : [
				pitches['C4'],
				pitches['D4'],
				pitches['E4'],
				pitches['G4'],
				pitches['A4'],
				pitches['C5']
			]
		};

		$scope.init = function() {
			$scope.stop();
			$scope.tracks = [];
		};
		$scope.params = function() {
			return null;
		};
		$scope.update = function(event, result) {
			$.each(result, function(id, data) {
				if ($.isArray(data) && data.length && data[0].time) {
					var useCounts = true;
					var unit = null;
					var values = $.map(data, function(item) {
						if (item.hasOwnProperty('avg')) {
							if (typeof item.avg === 'object') {
								unit = item.avg.unit;
								return item.avg['@value'];
							}
							useCounts = false;
							return item.avg;
						} else {
							return useCounts ? item.count : 0;
						}
					});
					$scope.tracks.push(normalize(values));
				}
			});
		};
		$scope.refresh = function(options, settings) {
			$scope.stop();
			$.extend($scope.settings, settings);
			if ($scope.tracks.length === 0) {
				$scope.$parent.refresh();
			}
		};
		$scope.play = function() {
			audio.resume();
			if ($scope.playing === 0) {
				$.each($scope.tracks, function(i, track) {
					play(track, scales[$scope.settings.scale], $scope.settings.tempo, 1.0 / $scope.tracks.length);
				});
			}
		};
		$scope.stop = function() {
			if (audio) {
				audio.suspend();
				audio.close();
			}
			audio = $window.AudioContext ? new $window.AudioContext() : new $window.webkitAudioContext();
			audio.suspend();
			var ticker = null;
			audio.onstatechange = function() {
				if (audio.state === 'running') {
					$scope.tic();
					ticker = $interval(function() {
						$scope.tic();
					}, Math.round(60000 / $scope.settings.tempo));
				} else if (ticker) {
					$interval.cancel(ticker);
					$scope.untic();
				}
			};
			$scope.playing = 0;
		};
		$scope.isRunning = function() {
			return audio.state === 'running';
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);

		function normalize(values) {
			var nonZeroValues = $.grep(values, function(value) { return value !== 0; });
			var min = Math.min.apply(null, nonZeroValues) || 0;
			values = $.map(values, function(value) {
				return Math.max(value - min, 0);
			});
			var max = Math.max.apply(null, values) || 1;
			return $.map(values, function(value) {
				return value / max;
			});
		}

		function play(notes, scale, tempo, volume) {
			for (var i = 0; i < notes.length; ++i) {
				var freq = notes[i] > 0 ? scale[Math.ceil(notes[i] * scale.length) - 1] : 0;
				var d = 60 / tempo;
				var t = audio.currentTime + i * d;
				var gain = audio.createGain();
				gain.connect(audio.destination);
				gain.gain.setValueAtTime(0.0, t);
				gain.gain.linearRampToValueAtTime(volume, t + d / 5);
				gain.gain.linearRampToValueAtTime(0.0, t + d);
				var osc = audio.createOscillator();
				osc.frequency.value = freq;
				osc.connect(gain);
				osc.start(t);
				osc.stop(t + d + d / 5);
				osc.onended = ended;
				++$scope.playing;
			}
		}

		function ended() {
			$scope.$apply(function() {
				if (--$scope.playing === 0) {
					audio.suspend();
				}
			});
		}
	}]);

	app.controller('SonificationWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', function($scope, WidgetDialogControllerSupport) {
		new WidgetDialogControllerSupport($scope);
		$scope.scales = [ 'chromatic', 'octatonic', 'pentatonic' ];
	}]);

	app.factory('Event', function() {

		var Event = function(data) {
			$.extend(true, this, data);
		};

		Event.prototype.get = function(fields) {
			var self = this;
			var entries = [];
			$.each(fields, function(i, field) {
				var value = self[field.name];
				if (value !== undefined) {
					$.each($.isArray(value) ? value : [ value ], function(i, value) {
						entries.push({ field : field, value : value });
					});
				}
			});
			return entries;
		};

		Event.prototype.add = function(field, value) {
			var values = this[field.name];
			if (values === undefined) {
				values = this[field.name] = [];
			} else if (!$.isArray(values)) {
				values = this[field.name] = [ values ];
				this[field.name] = values;
			}
			values.push(value);
		};

		return Event;
	});

	app.controller('EventDialogController', ['$scope', '$http', '$routeParams', 'Event', 'Field', 'tracker', 'delay', 'moment', function($scope, $http, $routeParams, Event, Field, tracker, delay, moment) {

		$scope.params = $routeParams;
		$scope.fields = Field.findEditable();
		$scope.init = function(event) {
			$scope.event = new Event(event);
			$scope.entries = $scope.event.get($scope.fields);
			$scope.isNew = $.isEmptyObject($scope.entries);
			$scope.message = '';
			$scope.field = null;
			$scope.value = '';
			$scope.$watch('event', function(event) {
				$scope.entries = event.get($scope.fields);
			}, true);
			tracker.event('dialog', $scope.isNew ? 'create event' : 'edit event');
		};
		$scope.getTemplate = function(field) {
			return field ? '/create-' + field.name + '.html' : null;
		};
		$scope.save = function() {
			if (!$scope.event['timestamp']) {
				$scope.event.add(Field.find('timestamp'), moment().format('YYYY-MM-DDTHH:mm:ss.000Z'));
			}
			$scope.alert.clear();
			if ($scope.isNew) {
				$http.post('/buckets/' + $scope.params.bucketId + '/', $scope.event)
					.success(function() {
						$scope.closeDialog();
						delay($scope.refresh);
					})
					.error(function(response) {
						$scope.message = response.message || 'Couldn\'t create this event.';
					});
			} else {
				$http.put('/buckets/' + $scope.params.bucketId + '/' + $scope.event['@id'], $scope.event)
					.success(function(response, status, headers) {
						$scope.closeDialog();
						$scope.alert.show('Updated an event.', 'alert-success', headers('X-Command-ID'));
						delay($scope.refresh);
					})
					.error(function(response) {
						$scope.message = response.message || 'Couldn\'t update this event.';
					});
			}
			tracker.event('action', 'save event');
		};
		$scope.remove = function(entry) {
			var values = $scope.event[entry.field.name];
			if ($.isArray(values)) {
				values = $.grep(values, function(value) {
					return value !== entry.value;
				});
				if (values.length === 1) {
					$scope.event[entry.field.name] = values[0];
				} else if (values.length > 0) {
					$scope.event[entry.field.name] = values;
				} else {
					delete $scope.event[entry.field.name];
				}
			} else {
				delete $scope.event[entry.field.name];
			}
		};
		$scope.reset = function() {
			$scope.field = null;
		};
	}]);

	app.controller('CreateTagFieldController', ['$scope', '$http', function($scope, $http) {

		var input = $('#tag-value-field');

		$scope.init = function() {
			$scope.value = '';
		};
		$scope.addField = function() {
			$scope.value = $.trim(input.val());
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return $scope.value;
		};

		$scope.init();
		if ($scope.total > 0) {
			$http.get('/buckets/' + $scope.bucket['@id'] + '/tags/')
			.success(function(response) {
				input.typeahead({ source : response });
			});
		}
	}]);

	app.controller('CreateLocationFieldController', ['$scope', function($scope) {

		function parseLatLng(value) {
			var p = value.indexOf(',');
			if (p == -1) {
				return null;
			}
			var lat = parseFloat(value.substring(0, p));
			var lng = parseFloat(value.substring(p + 1));
			return !isNaN(lat) && !isNaN(lng) ? new google.maps.LatLng(lat, lng) : null;
		}

		$scope.init = function() {
			var center = new google.maps.LatLng(0, 0);
			var options = {
				center : center,
				zoom : 2,
				mapTypeId: google.maps.MapTypeId.ROADMAP,
				streetViewControl: false,
				draggableCursor : 'crosshair',
				mapTypeControlOptions : {
					style : google.maps.MapTypeControlStyle.DROPDOWN_MENU
				}
			};
			$scope.map = new google.maps.Map(document.getElementById('create-location-map'), options);
			google.maps.event.addListener($scope.map, 'click', function(e) {
				$scope.moveMarker(e.latLng);
			});
			var input = $('#location-search-field');
			input.on('input', function() {
				var latLng = parseLatLng(input.val());
				if (latLng) {
					$scope.moveMarker(latLng);
					$scope.map.setCenter(latLng);
				}
			});
			var autocomplete = new google.maps.places.Autocomplete(input.get(0));
			autocomplete.bindTo('bounds', $scope.map);
			google.maps.event.addListener(autocomplete, 'place_changed', function() {
				var place = autocomplete.getPlace();
				if (place.geometry) {
					if (place.geometry.viewport) {
						$scope.map.fitBounds(place.geometry.viewport);
					}
					if (place.geometry.location) {
						$scope.moveMarker(place.geometry.location);
						$scope.map.setCenter(place.geometry.location);
					}
				}
			});

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(function(position) {
					var latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
					$scope.moveMarker(latLng);
					$scope.map.setCenter(latLng);
					$scope.map.setZoom(10);
				});
			}
		};
		$scope.moveMarker = function(latLng) {
			if ($scope.marker) {
				$scope.marker.setPosition(latLng);
			} else {
				$scope.marker = new google.maps.Marker({
					position : latLng,
					map : $scope.map,
					title : 'Location',
					draggable: true
				});
				google.maps.event.addListener($scope.marker, 'dragend', function() {
					$scope.setValue($scope.marker.getPosition());
				});
			}
			$scope.setValue(latLng);
		};
		$scope.setValue = function(latLng) {
			$scope.$apply(function() {
				$scope.value = {
						lat : latLng.lat(),
						lon : latLng.lng()
				};
			});
		};
		$scope.valid = function() {
			return $scope.value && $scope.value.lat >= -90 && $scope.value.lat <= 90 &&
				$scope.value.lon >= -180 && $scope.value.lon <= 180;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};

		$scope.init();
	}]);


	app.controller('CreateTimestampFieldController', ['$scope', 'timezone', 'moment', function($scope, timezone, moment) {

		$scope.timezones = [
			'-12:00', '-11:00', '-10:00', '-09:30', '-09:00', '-08:00', '-07:00', '-06:00','-05:00', '-04:30', '-04:00', '-03:00', '-02:00', '-01:00', 'Z',
			'+01:00', '+02:00', '+03:00', '+04:00', '+04:30', '+05:00', '+05:30', '+05:45', '+06:00', '+06:30', '+07:00', '+08:00', '+08:45', '+09:00', '+09:30', '+10:00', '+11:00', '+11:30', '+12:00', '+12:45', '+13:00', '+14:00'
		];

		function getValue() {
			var day = (typeof $scope.date === 'object') ? moment(local($scope.date)).format('YYYY-MM-DD') : $scope.date;
			var time = $scope.time.format('HH:mm:ss.SSS');
			return day + 'T' + time + $scope.timezone;
		}
		function local(date) {
			return new Date(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
		}
		function utc(date) {
			return new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
		}

		$scope.init = function() {
			$scope.date = utc(new Date());
			$scope.time = moment().seconds(0).milliseconds(0);
			$scope.timezone = timezone;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, getValue());
			$scope.reset();
		};
		$scope.valid = function() {
			return moment(getValue()).isValid();
		};

		$scope.init();
	}]);

	app.controller('CreateDurationFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.days = $scope.hours = $scope.minutes = $scope.seconds = 0;
		};
		$scope.millis = function() {
			return ((($scope.days * 24 + $scope.hours) * 60 + $scope.minutes) * 60 + $scope.seconds) * 1000;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.millis());
			$scope.reset();
		};
		$scope.valid = function() {
			return $scope.millis() > 0;
		};

		$scope.init();
	}]);

	app.controller('CreatePaceFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.minutes = $scope.seconds = 0;
			$scope.unit = null;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, {
				'@value' : $scope.minutes * 60 + $scope.seconds,
				'unit' : 's/' + $scope.unit
			});
			$scope.reset();
		};
		$scope.getUnits = function() {
			return $.map($scope.field.units, function(unit) {
				return unit.substring(2);
			});
		};
		$scope.valid = function() {
			return ($scope.minutes + $scope.seconds) > 0 && $scope.unit;
		};

		$scope.init();
	}]);

	app.controller('CreateResourceFieldController', ['$scope', '$http', function($scope, $http) {

		$scope.init = function() {
			$scope.value = {};
			$scope.loading = false;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.prefillTitle = function() {
			if ($scope.value.url) {
				$scope.loading = true;
				$http.get('/og?' + $.param({ url : $scope.value.url }))
					.success(function(response) {
						$scope.value.title = response.title;
						$scope.loading = false;
					})
					.error(function() {
						$scope.loading = false;
					});
			}
		};
		$scope.valid = function() {
			return $scope.value.url && $scope.value.title;
		};
		$scope.change = function() {

		};
		$scope.$watch('value.url', function(url) {
			if (url && !$scope.value.title) {
				$scope.prefillTitle();
			}
		});
		$scope.init();
	}]);

	app.controller('CreateUnitFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = {};
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.getUnits = function() {
			return $scope.field.units;
		};
		$scope.valid = function() {
			return $.isNumeric($scope.value['@value']) && $scope.value.unit;
		};

		$scope.init();
	}]);

	app.controller('CreateIntegerFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = 0;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return /^\d+$/.test($scope.value);
		};

		$scope.init();
	}]);

	app.controller('CreateRatingFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.stars = 0;
			$scope.highlighted = 0;
		};
		$scope.highlight = function(stars) {
			$scope.highlighted = stars;
		};
		$scope.set = function(stars) {
			$scope.stars = stars;
		};
		$scope.get = function() {
			return $scope.highlighted || $scope.stars;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.stars * 20);
			$scope.reset();
		};

		$scope.init();
	}]);

	app.controller('CreatePercentageFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = 0;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return $.isNumeric($scope.value);
		};

		$scope.init();
	}]);

	app.controller('CreateMoonFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = 0;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return $.isNumeric($scope.value);
		};

		$scope.init();
	}]);

	app.controller('CreateCurrencyFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = 0.00;
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return $.isNumeric($scope.value);
		};

		$scope.init();
	}]);

	app.controller('CreateNoteFieldController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = '';
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.valid = function() {
			return $scope.value;
		};

		$scope.init();
	}]);

	app.factory('EventSpreadsheet', function() {

		/**
		 * input: timestamp, distance.@value, distance.unit, tag
		 * output: { 'timestamp' : 0, 'distance' : { '@value' : 1, 'unit' : 2 }, 'tag' : 3 }
		 */
		function buildMappings(headers) {
			var mappings = {};
			$.each(headers, function(i, header) {
				var path = header.split('.', 2);
				var mapping;
				if (path.length == 1) {
					mapping = i;
				} else {
					mapping = mappings[path[0]] || {};
					mapping[path[1]] = i;
				}
				mappings[path[0]] = mapping;
			});
			delete mappings['@id'];
			delete mappings['author'];
			delete mappings['version'];
			return mappings;
		}

		return {
			parse : function(s) {
				var events = [];
				var csv = Baby.parse(s, { skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				var mappings = buildMappings(csv.data.shift());
				$.each(csv.data, function(i, row) {
					var event = {};
					$.each(mappings, function(field, mapping) {
						var value;
						if ($.isNumeric(mapping)) {
							value = row[mapping];
							if (angular.isDefined(value) && value !== '') {
								event[field] = value.split(';');
							}
						} else {
							var objects = 1;
							for (var i = 0; i < objects; ++i) {
								var object = {};
								for (var nested in mapping) {
									var offset = mapping[nested];
									value = row[offset];
									if (angular.isDefined(value) && value !== '') {
										var values = value.split(';');
										object[nested] = values[i];
										objects = values.length;
									}
								}
								if (!$.isEmptyObject(object)) {
									event[field] = event[field] || [];
									event[field].push(object);
								}
							}
						}
					});
					if (!$.isEmptyObject(event)) {
						events.push(event);
					}
				});
				return events;
			}
		};
	});

	app.factory('HealthKit', [ 'moment', function(moment) {

		return {
			parse : function(s) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var f = 'DD/MMM/YYYY H:mm:ss';
					var t0 = moment(row['Start'], f);
					var t1 = moment(row['Finish'], f);
					var duration = t1.valueOf() - t0.valueOf();
					var timestamp = t0.format('YYYY-MM-DDTHH:mm:ss.SSSZ');
					var eventsByTag = {};
					function push(tag, field, value) {
						var event = eventsByTag[tag];
						if (!event) {
							event = {
								'timestamp' : timestamp,
								'duration' : duration,
								'tag' : [ tag ]
							};
							events.push(eventsByTag[tag] = event);
						}
						(event[field] = event[field] || []).push(value);
					}
					for (var field in row) {
						var value = Number(row[field]);
						var m = field.match(/(.+) \((.+?)\)/);
						if (m && !isNaN(value) && value !== 0) {
							var tag = m[1].replace(/ \(.+\)/, ''); // "Blood Pressure (Diastolic)" -> "Blood Pressure"
							var unit = m[2];
							switch (unit) {
								case 'count':
									push(tag, 'count', Math.round(value));
									break;
								case '%':
									push(tag, 'percentage', Math.round(100.0 * value));
									break;
								case 'mcg':
									unit = 'ug';
									/* falls through */
								case 'mg':
								case 'g':
								case 'kg':
								case 'lb':
									push(tag, 'weight', {
										'@value' : value,
										'unit' : unit
									});
									break;
								case 'L':
									push(tag, 'volume', {
										'@value' : value,
										'unit' : unit
									});
									break;
								case 'mg/dL':
									push(tag, 'concentration', {
										'@value' : value,
										'unit' : unit
									});
									break;
								case 'mmHg':
									push(tag, 'pressure', {
										'@value' : value,
										'unit' : unit
									});
									break;
								case 'degC':
									push(tag, 'temperature', {
										'@value' : value,
										'unit' : 'C'
									});
									break;
								case 'cal':
								case 'kcal':
									push(tag, 'energy', {
										'@value' : value,
										'unit' : unit
									});
									break;
								case 'count/min':
									push(tag, 'frequency', {
										'@value' : value,
										'unit' : 'bpm'
									});
									break;
							}
						}
					}
				});
				return events;
			}
		};
	}]);

	app.factory('SleepCycle', ['moment', function(moment) {

		function formatMoment(t) {
			return t.format('YYYY-MM-DDTHH:mm:ss.SSSZ');
		}

		function parseStart(value) {
			var t = moment(value);
			if (!t) {
				throw new Error(value + ' is not a valid time');
			}
			return t;
		}

		function parseSleepQuality(value) {
			var m = /(\d+)%/.exec(value);
			if (m) {
				return parseInt(m[1]);
			}
			throw new Error(value + ' is not a valid sleep quality');
		}

		function parseTimeInBed(value) {
			var m = /(\d+):(\d+)/.exec(value);
			if (m) {
				return parseInt(m[1]) * 60 * 60 * 1000 + parseInt(m[2]) * 60 * 1000;
			}
			throw new Error(value + ' is not a valid duration');
		}

		function parseWakeUp(value) {
			if (!value) {
				return undefined;
			}
			if (value === ':)') {
				return 100;
			}
			if (value === ':|') {
				return 50;
			}
			if (value === ':(') {
				return 0;
			}
			throw new Error(value + ' is not a valid emoticon');
		}

		function parseSleepNotes(values) {
			var tags = [ 'sleep' ];
			if (values) {
				$.each(values.split(':'), function(i, value) {
					tags.push(value.toLowerCase());
				});
			}
			return tags;
		}

		return {
			parse : function(data) {
				var events = [];
				var lines = data.split(/[\r\n]+/g);
				var expected = [ 'Start', 'End', 'Sleep quality', 'Time in bed', 'Wake up', 'Sleep Notes' ];
				var headers = lines.shift().split(';').slice(0, 6);
				if (!angular.equals(headers, expected)) {
					throw new Error('Expected headers: ' + expected.join(', '));
				}
				$.each(lines, function(i, line) {
					var fields = line.split(';');
					if (line.trim()) {
						if (fields.length < expected.length) {
							throw new Error('Wrong number of fields in line ' + i);
						}
						var begin = parseStart(fields[0]);
						var duration = parseTimeInBed(fields[3]);
						var event = {
							'tag' : parseSleepNotes(fields[5]),
							'timestamp' : [ formatMoment(begin), formatMoment(begin.add(duration, 'ms')) ],
							'duration' : duration,
							'percentage' : parseSleepQuality(fields[2]),
							'rating' : parseWakeUp(fields[4]),
							'source' : {
								'title' : 'SleepCycle',
								'url' : 'https://www.sleepcycle.com/'
							}
						};
						events.push(event);
					}
				});
				return events;
			}
		};
	}]);

	app.factory('TapLog', function() {

		return {
			parse : function(s, settings) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var event = {
						'timestamp' : row['timestamp'],
						'tag' : [],
					};
					for (var i = 1; i < 10; ++i) {
						var category = row['cat' + i];
						if (category) {
							event['tag'].push(category);
						}
					}
					if (settings.field && row['number']) {
						var value = Number(row['number']);
						if (settings.field === 'rating') {
							value *= 20;
						}
						event[settings.field] = settings.unit ? {
							'@value' : value,
							'unit' : settings.unit
						} : value;
					}
					if (row['note']) {
						event['note'] = row['note'];
					}
					if (row['latitude']) {
						var lat = Number(row['latitude']);
						var lon = Number(row['longitude']);
						if (lat !== 0 || lon !== 0) {
							event['location'] = { 'lat' : lat, 'lon' : lon };
						}
					}
					events.push(event);
				});
				return events;
			}
		};
	});

	app.factory('Nomie', [ 'moment', function(moment) {

		function parseJSON(s, settings) {
			var events = [];
			var data = JSON.parse(s);
			var tags = {};
			var add = function(i, item) {
				var event = {
					'timestamp' : moment(item.time).utcOffset(-item.offset).format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
					'tag' : tags[item.parent],
					'rating' : (Number(item.charge) + 3) * 20
				};
				if (item.geo.length === 2) {
					event['location'] = { 'lat' : Number(item.geo[0]), 'lon' : Number(item.geo[1]) };
				}
				if (typeof item.value === 'string') {
					event['note'] = item.value;
				} else if (typeof item.value === 'number' && settings.field) {
					event[settings.field] = settings.unit ? {
						'@value' : item.value,
						'unit' : settings.unit
					} : item.value;
				}
				events.push(event);
			};
			$.each(data.trackers, function(i, tracker) {
				tags[tracker._id] = [ tracker.label ];
				if (tracker.groups) {
					$.each(tracker.groups, function(i, group) {
						if (group) {
							tags[tracker._id].push(group);
						}
					});
				}
			});
			$.each(data.ticks, add);
			$.each(data.notes, add);
			return events;
		}

		function parseCSV(s, settings) {
			var events = [];
			var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
			if (csv.errors.length) {
				throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
			}
			$.each(csv.data, function(rowNum, row) {
				var t = moment(row['iso_date']);
				var offset = Number(row['offset']);
				if (isFinite(offset)) {
					t.utcOffset(-offset);
				}
				var timestamp = t.format('YYYY-MM-DDTHH:mm:ss.SSSZ');
				var event = {
					'timestamp' : timestamp,
					'tag' : [],
				};
				if (row['tracker'] === '') {
					return;
				} else if (row['tracker'] !== 'Unknown') {
					event['tag'].push(row['tracker']);
				}
				if (row['charge'] !== undefined) {
					event['rating'] = (Number(row['charge']) + 3) * 20;
				}
				var value = Number(row['value']);
				if (value && settings.field) {
					event[settings.field] = settings.unit ? {
						'@value' : value,
						'unit' : settings.unit
					} : value;
				}
				if (row['lat']) {
					var lat = Number(row['lat']);
					var lon = Number(row['long']);
					if (lat !== 0 || lon !== 0) {
						event['location'] = { 'lat' : lat, 'lon' : lon };
					}
				}
				events.push(event);
			});
			return events;
		}

		return {
			parse : function(s, settings) {
				return s.charAt(0) === '{' ? parseJSON(s, settings) : parseCSV(s, settings);
			}
		};
	}]);

	app.factory('Nomie5', [ 'moment', function(moment) {

		function parseCSV(s, settings) {
			var events = [];
			var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
			if (csv.errors.length) {
				throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
			}
			$.each(csv.data, function(rowNum, row) {
				var t0 = moment(row['start']);
				var t1 = moment(row['end']);
				var offset = Number(row['offset']);
				if (isFinite(offset)) {
					t0.utcOffset(-offset);
					t1.utcOffset(-offset);
				}
				var event = {
					'timestamp' : [
						t0.format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
						t1.format('YYYY-MM-DDTHH:mm:ss.SSSZ')
					],
					'tag' : [],
				};
				if (row['tracker'] === '') {
					return;
				} else if (row['tracker'] !== 'Unknown') {
					event['tag'].push(row['tracker']);
				}
				var value = Number(row['value']);
				if (value && settings.field) {
					event[settings.field] = settings.unit ? {
						'@value' : value,
						'unit' : settings.unit
					} : value;
				}
				if (row['lat']) {
					var lat = Number(row['lat']);
					var lon = Number(row['lng']);
					if (lat !== 0 || lon !== 0) {
						event['location'] = { 'lat' : lat, 'lon' : lon };
					}
				}
				events.push(event);
			});
			return events;
		}

		return {
			parse : function(s, settings) {
				return parseCSV(s, settings);
			}
		};
	}]);

	app.factory('Basis', [ 'moment', function(moment) {

		function meanOfNonZeroValues(values) {
			var sum = 0;
			var count = 0;
			for (var i = 0; i < values.length; ++i) {
				if (values[i]) {
					++count;
					sum += values[i];
				}
			}
			return sum / count;
		}

		return {
			parse : function(s, settings) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				var hour = null;
				var rows = 0;
				var count = 0;
				var energy = 0.0;
				var frequencies = [];
				var temperatures = [];
				function push() {
					var event = {
						'timestamp' : hour,
						'duration' : 3600000,
						'tag' : [ settings.tag ],
						'energy' : {
							'@value' : Math.round(10 * energy) / 10,
							'unit' : 'kcal'
						},
						'count' : count,
						'source' : {
							'title' : 'Basis',
							'url' : 'https://www.mybasis.com/'
						}
					};
					var frequency = meanOfNonZeroValues(frequencies);
					if (isFinite(frequency)) {
						event['frequency'] = {
							'@value' : Math.round(frequency),
							'unit' : 'bpm'
						};
					}
					var temperature = meanOfNonZeroValues(temperatures);
					if (isFinite(temperature)) {
						event['temperature'] = {
							'@value' : Math.round(10 * temperature) / 10,
							'unit' : 'F'
						};
					}
					events.push(event);
					hour = null;
					rows = 0;
					count = 0;
					energy = 0.0;
					frequencies = [];
					temperatures = [];
				}
				$.each(csv.data, function(rowNum, row) {
					var t = moment(row['date']);
					var h = t.format('YYYY-MM-DDTHH:00:00.000Z');
					if (hour && hour !== h) {
						push();
					}
					++rows;
					hour = h;
					count += Number(row['steps'] || 0);
					energy += Number(row['calories'] || 0);
					if (row['heart-rate']) {
						frequencies.push(Number(row['heart-rate']));
					}
					if (row['skin-temp']) {
						temperatures.push(Number(row['skin-temp']));
					}
				});
				if (hour && rows) {
					push();
				}
				return events;
			}
		};
	}]);

	app.factory('SunSprite', [ 'moment', function(moment) {

		function mean(values) {
			var sum = 0;
			for (var i = 0; i < values.length; ++i) {
					if (values[i] >= 0) {
						sum += values[i];
					}
			}
			return sum / values.length;
		}

		return {
			parse : function(s, settings) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				var hour = null;
				var rows = 0;
				var luxes = [];
				var uvs = [];
				function push() {
					var event = {
						'timestamp' : hour,
						'duration' : 3600000,
						'tag' : [ settings.tag ],
						'source' : {
							'title' : 'SunSprite',
							'url' : 'https://www.sunsprite.com/'
						}
					};
					var lux = mean(luxes);
					if (isFinite(lux)) {
						event['light'] = {
							'@value' : Math.round(lux),
							'unit' : 'lx'
						};
					}
					var uv = mean(uvs);
					if (isFinite(uv)) {
						event['rating'] = Math.max(0, 100 - Math.round(10 * uv));
					}
					events.push(event);
					hour = null;
					rows = 0;
					luxes = [];
					uvs = [];
				}
				$.each(csv.data, function(rowNum, row) {
					var t = moment.tz(row['date'], settings.timezone);
					var h = t.format('YYYY-MM-DDTHH:00:00.000Z');
					if (hour && hour !== h) {
						push();
					}
					++rows;
					hour = h;
					luxes.push(Number(row['lux']));
					uvs.push(Number(row['uv index']));
				});
				if (hour && rows) {
					push();
				}
				return events;
			}
		};
	}]);

	app.factory('HabitBull', [ 'moment', function(moment) {

		return {
			parse : function(s) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var event = {
						'timestamp' : moment(row['CalendarDate']).format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
						'tag' : [ row['HabitName'] ],
						'count' : Math.round(Number(row['Value']))
					};
					if (row['CommentText']) {
						event['note'] = row['CommentText'];
					}
					events.push(event);
				});
				return events;
			}
		};
	}]);

	app.factory('SleepyHead', [ 'moment', function(moment) {

		return {
			parse : function(s, settings) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var t0 = moment.tz(row['Start'], settings.timezone);
					var t1 = moment.tz(row['End'], settings.timezone);
					var event = {
						'timestamp' : [
							t0.format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
							t1.format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
						],
						'duration' : t1.valueOf() - t0.valueOf(),
						'tag' : settings.tag,
						'rating' : Math.round(100 * Math.exp(-Number(row['AHI']) / 32)),
						'pressure' : {
							'@value' : Number(row['Pressure  Avg']),
							'unit' : 'cm_wg'
						}
					};
					events.push(event);
				});
				return events;
			}
		};
	}]);

	app.factory('MoodPanda', [ 'moment', function(moment) {

		return {
			parse : function(s, settings) {
				var events = [];
				var csv = Baby.parse(s, { header : true, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var event = {
						'timestamp' : moment.tz(row['Date'], 'DD/MM/YYYY HH:mm:ss', settings.timezone).format('YYYY-MM-DDTHH:mm:ss.SSSZ'),
						'tag' : [ settings.tag ],
						'rating' : Number(row['Rating']) * 10
					};
					if (row['Reason']) {
						event['note'] = row['Reason'];
					}
					events.push(event);
				});
				return events;
			}
		};
	}]);

	app.factory('LibreView', [ 'moment', function(moment) {

		return {
			parse : function(s, settings) {
				var lines = s.split("\n");
				if (lines.length > 0 && lines[0].startsWith('Export')) {
					lines.shift();
				}
				if (lines.length > 0 && lines[0].startsWith('Meter')) {
					lines.shift();
				}
				s = lines.join("\n");
				var events = [];
				var csv = Baby.parse(s, { header : false, skipEmptyLines : true });
				if (csv.errors.length) {
					throw new Error(csv.errors[0].message + ' in row ' + csv.errors[0].row);
				}
				$.each(csv.data, function(rowNum, row) {
					var t = moment.tz(row[2], 'MM-DD-YYYY LT', settings.timezone);
					var event = {
						'timestamp' : t.format('YYYY-MM-DDTHH:mm:00.000Z'),
						'tag' : [ settings.tag ],
						'source' : {
							'title' : row[0],
							'url' : 'https://www.libreview.com/'
						}
					};
					switch (row[3]) {
						case "0":
							event['tag'] = [ 'Glucose', 'Historic' ];
							event['concentration'] = {
								'@value' : Number(row[4]),
								'unit' : 'mg/dL'
							};
							break;
						case "1":
							event['tag'] = [ 'Glucose', 'Scan' ];
							event['concentration'] = {
								'@value' : Number(row[5]),
								'unit' : 'mg/dL'
							};
							break;
						case "2":
							event['tag'] = [ 'Glucose', 'Strip' ];
							event['concentration'] = {
								'@value' : Number(row[14]),
								'unit' : 'mg/dL'
							};
							break;
						case "6":
							event['tag'] = [ "Note" ];
							event['note'] = row[13];
							break;
						default:
							return;
					}
					events.push(event);
				});
				return events;
			}
		};
	}]);

	app.controller('ImportDialogController', ['$scope', '$http', '$routeParams', 'EventSpreadsheet', 'HealthKit', 'SleepCycle', 'SleepyHead', 'SunSprite', 'TapLog', 'MoodPanda', 'Nomie', 'Nomie5', 'Basis', 'HabitBull', 'LibreView', 'tracker', 'delay', function($scope, $http, $routeParams, EventSpreadsheet, HealthKit, SleepCycle, SleepyHead, SunSprite, TapLog, MoodPanda, Nomie, Nomie5, Basis, HabitBull, LibreView, tracker, delay) {

		$scope.bucketId = $routeParams.bucketId;
		$scope.formats = [
			{
				id : 'zenobase',
				label : 'Zenobase',
				description : 'Import a <b>.json</b> or <b>.csv</b> file exported from another bucket.<br/>The fields are described in the <a href="/#/api/events" target="_blank">API docs</a>.',
				parse : function(data) {
					if (data.charAt(0) === '{' || data.charAt(0) === '[') {
						var events = JSON.parse(data);
						if (events && events.events) {
							events = events.events;
						}
						return $.isArray(events) ? events : [];
					} else {
						return EventSpreadsheet.parse(data);
					}
				}
			},
			{
				id : 'basis',
				label : 'Basis',
				description : 'Import a <b>.csv</b> file from <a href="https://www.mybasis.com/" target="_blank">Basis</a>.',
				parse : function(data) {
					return Basis.parse(data, { tag : 'Basis' });
				}
			},
			{
				id : 'habitbull',
				label : 'HabitBull',
				description : 'Import a <b>.csv</b> file from <a href="http://www.habitbull.com/" target="_blank">HabitBull</a>.',
				parse : function(data) {
					return HabitBull.parse(data);
				}
			},
			{
				id : 'healthkit',
				label : 'HealthKit',
				description : 'Import HealthKit data from a <b>.csv</b> file exported with the <a href="http://quantifiedself.com/access-app/app" target="_blank">QS Access</a> app.',
				parse : function(data, settings) {
					return HealthKit.parse(data, settings);
				}
			},
			{
				id : 'libreview',
				label : 'LibreView',
				description : 'Import a <b>.csv</b> file containing blood sugar readings and notes from <a href="https://www.libreview.com/" target="_blank">LibreView</a>.',
				settings : '/import-libreview.html',
				parse : function(data, settings) {
					return LibreView.parse(data, settings);
				}
			},
			{
				id : 'nomie',
				label : 'Nomie',
				description : 'Import a <b>.json</b> or <b>.csv</b> file from <a href="https://nomie.io/" target="_blank">Nomie</a>.',
				settings : '/import-nomie.html',
				parse : function(data, settings) {
					return Nomie.parse(data, settings);
				}
			},
			{
				id : 'nomie5',
				label : 'Nomie 5',
				description : 'Import a <b>.csv</b> file from <a href="https://nomie.io/" target="_blank">Nomie</a>.',
				settings : '/import-nomie.html',
				parse : function(data, settings) {
					return Nomie5.parse(data, settings);
				}
			},
			{
				id : 'moodpanda',
				label : 'MoodPanda',
				description : 'Import a <b>.csv</b> file from <a href="https://moodpanda.com/" target="_blank">MoodPanda</a>.',
				settings : '/import-moodpanda.html',
				parse : function(data, settings) {
					return MoodPanda.parse(data, settings);
				}
			},
			{
				id : 'sleepcycle',
				label : 'SleepCycle',
				description : 'Import a <b>.csv</b> file from <a href="https://www.sleepcycle.com/" target="_blank">SleepCycle</a>.',
				parse : function(data) {
					return SleepCycle.parse(data);
				}
			},
			{
				id : 'sleepyhead',
				label : 'SleepyHead',
				description : 'Import a <b>.csv</b> file from <a href="https://sleepyhead.jedimark.net/" target="_blank">SleepyHead</a>.',
				settings : '/import-sleepyhead.html',
				parse : function(data, settings) {
					return SleepyHead.parse(data, settings);
				}
			},
			{
				id : 'sunsprite',
				label : 'SunSprite',
				description : 'Import a <b>.csv</b> file from <a href="https://www.sunsprite.com/" target="_blank">SunSprite</a>.',
				settings : '/import-sunsprite.html',
				parse : function(data, settings) {
					return SunSprite.parse(data, settings);
				}
			},
			{
				id : 'taplog',
				label : 'TapLog',
				description : 'Import a <b>.csv</b> file from <a href="https://welcome.taplog.info/" target="_blank">TapLog</a>.',
				settings : '/import-taplog.html',
				parse : function(data, settings) {
					return TapLog.parse(data, settings);
				}
			}
		];

		$scope.init = function(formatId) {
			$scope.importing = false;
			$scope.settings = {};
			$scope.message = '';
			$scope.events = [];
			$scope.offset = 0;
			$scope.clearFiles();
			$scope.format = $scope.formats[0];
			if (formatId) {
				$.each($scope.formats, function(i, format) {
					if (format.id === formatId) {
						$scope.format = format;
						return false;
					}
				});
			}
			tracker.event('dialog', 'import events');
		};
		$scope.isEmpty = function() {
			return !$scope.events || $scope.events.length === 0;
		};
		$scope.prev = function() {
			--$scope.offset;
		};
		$scope.next = function() {
			++$scope.offset;
		};
		$scope.setFiles = function(files) {
			$scope.message = '';
			$scope.events = [];
			$scope.offset = 0;
			$scope.$apply(function(scope) {
				var reader = new FileReader();
				reader.onload = function(e) {
					scope.$apply(function(scope) {
						try {
							scope.events = $scope.format.parse(e.target.result, $scope.settings);
						} catch(error) {
							scope.message = error.message;
							$scope.settings = {};
							$scope.clearFiles();
							throw error;
						}
					});
				};
				if (files.length) {
					reader.readAsText(files[0], $scope.settings.encoding);
				}
			});
		};
		$scope.submit = function() {
			$scope.alert.clear();
			$scope.importing = true;
			$http.post('/buckets/' + $scope.bucketId + '/', { 'events' : $scope.events })
				.success(function(response, status, headers) {
					$scope.alert.show('Imported events.', 'alert-success', headers('X-Command-ID'));
					$scope.settings = {};
					$scope.events = [];
					$scope.offset = 0;
					delay($scope.refresh);
					$scope.closeDialog();
					$scope.importing = false;
				})
				.error(function(response) {
					$scope.message = response.message || 'Couldn\'t import the file. Try again later, or contact support.';
					$scope.settings = {};
					$scope.events = [];
					$scope.offset = 0;
					$scope.clearFiles();
					$scope.importing = false;
				});
			tracker.event('action', 'import events');
		};
		$scope.clearFiles = function() {
			$('#select-import-file').fileupload('reset');
		};

		$scope.$watch('format', function(format) {
			if (format) {
				$scope.settings = {};
				$scope.message = '';
				$scope.events = [];
				$scope.offset = 0;
				$scope.clearFiles();
			}
		});
	}]);

	app.controller('ImportTapLogController', ['$scope', 'Field', function($scope, Field) {

		$scope.fields = Field.findByType('numeric');
		$scope.units = [];

		$scope.$watch('settings.field', function() {
			$scope.units = $scope.settings.field && Field.find($scope.settings.field).units || [];
			$scope.settings.unit = $scope.units.length ? $scope.units[0] : null;
		});
		$scope.settings = $scope.$parent.settings;
	}]);

	app.controller('ImportSunSpriteController', ['$scope', function($scope) {

		$scope.settings.tag = 'Sunlight';
		$scope.settings.timezone = 'UTC';

		$scope.settings = $scope.$parent.settings;
	}]);

	app.controller('ImportLibreViewController', ['$scope', function($scope) {

		$scope.settings.timezone = 'UTC';
		$scope.settings.encoding = 'UTF-16LE';

		$scope.settings = $scope.$parent.settings;
	}]);

	app.controller('ImportNomieController', ['$scope', 'Field', function($scope, Field) {

		$scope.fields = Field.findByType('numeric');
		$scope.units = [];

		$scope.$watch('settings.field', function() {
			$scope.units = $scope.settings.field && Field.find($scope.settings.field).units || [];
			$scope.settings.unit = $scope.units.length ? $scope.units[0] : null;
		});
		$scope.settings = $scope.$parent.settings;
	}]);

	app.controller('ImportSleepyHeadController', ['$scope', function($scope) {

		$scope.settings.tag = 'sleep';
		$scope.settings.timezone = 'UTC';

		$scope.settings = $scope.$parent.settings;
	}]);

	app.controller('ImportMoodPandaController', ['$scope', function($scope) {

		$scope.settings.tag = 'Mood';
		$scope.settings.timezone = 'UTC';

		$scope.settings = $scope.$parent.settings;
	}]);


	app.controller('ExportDialogController', ['$scope', '$routeParams', '$window', 'token', 'tracker', function($scope, $routeParams, $window, token, tracker) {

		$scope.csvLimit = 16000;
		$scope.bucketId = $routeParams.bucketId;

		$scope.init = function() {
			if ($scope.total > $scope.csvLimit) {
				$scope.message = '<b>csv</b> export is limited to ' + $scope.csvLimit + ' events; add one or more constraints to enable.';
			} else if ($scope.constraints.length > 0) {
				$scope.message = 'Only events matching the current constraints will be exported.';
			} else {
				$scope.message = '';
			}
			$scope.media = 'json';
			tracker.event('dialog', 'export events');
		};
		$scope.url = function() {
			var url = '/buckets/' + $scope.bucketId + '/';
			var params = {};
			if ($scope.constraints.length > 0) {
				params.q = $scope.constraints;
			}
			if (token.get()) {
				params.code = token.get();
			}
			if ($scope.media === 'csv') {
				params.accept = 'text/plain';
			}
			if (!$.isEmptyObject(params)) {
				url += '?' + $.param(params, true);
			}
			return url;
		};
		$scope.file = function() {
			return $scope.bucketId + '.' + $scope.media;
		};
		$scope.submit = function() {
			$scope.alert.clear();
			$scope.closeDialog();
			tracker.event('action', 'export events', { 'Format': $scope.format });
		};
	}]);

	app.factory('taskRunner', [ '$http', '$q', '$window', 'localStorage', function($http, $q, $window, localStorage) {

		var runAll = function($scope, bucketId) {
			return $http.get('/buckets/' + bucketId + '/tasks/').then(function(response) {
				var previous = $q.when(null); // run tasks sequentially
				$.each(response.data.tasks, function(i, task) {
					previous = previous.then(function() {
						return runOne($scope, task['@id']);
					}, function() {
						return $q.reject();
					});
				});
				return previous;
			});
		};

		var runOne = function($scope, taskId) {
			return $http.get('/tasks/' + taskId)
				.then(function(response) {
					if (response.headers('X-Credentials')) {
						return newCredentials($scope, response.headers('X-Credentials'));
					} else if (response.headers('Link')) {
						var match = response.headers('Link').match(/<(.+?)>/);
						console.assert(match, 'Invalid Link header: ' + response.headers('Link'));
						authorize($scope, null, response.data.type, match[1]);
						return $q.reject();
					}
				}, function(response) {
					if (response.status == 403) {
						$scope.alert.show('Couldn\'t refresh a task. Insufficient quota?', 'alert-error');
					} else if (response.status < 500) {
						$scope.alert.show('Couldn\'t refresh a task.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t refresh a task. Try again later or contact support.', 'alert-error');
					}
					return $q.reject();
				});
		};

		var newCredentials = function($scope, type) {
			return $http.post('/credentials/', { type : type }).then(function(response) {
					console.assert(response.status === 201, response.status);
					if (response.data.authorizationUrl) {
						authorize($scope, response.data['@id'], type, response.data.authorizationUrl);
					}
					return $q.reject();
				}, function(response) {
					if (response.status === 400) {
						$scope.alert.show('Can\'t create credentials: ' + response.data.message, 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t create credentials. Please try again later or contact support.', 'alert-error');
					}
					return $q.reject();
				});
		};

		var authorize = function($scope, credentialsId, type, url) {
			$scope.alert.show('<b>' + type + '</b> requires authorization', '', '', function() {
				if (credentialsId && url.indexOf(credentialsId) == -1) {
					localStorage.setItem('credentials', credentialsId);
				}
				$window.open(url);
			});
		};

		return {
			runAll : runAll,
			runOne : runOne
		};
	}]);

	app.controller('TaskListDialogController', ['$scope', '$http', 'tracker', 'delay', function($scope, $http, tracker, delay) {

		$scope.init = function() {
			$scope.message = '';
			$scope.offset = 0;
			$scope.limit = 10;
			$scope.total = 0;
			$scope.tasks = null;
			$scope.refresh();
			tracker.event('dialog', 'list tasks');
		};

		$scope.hasPrev = function() {
			return $scope.offset > 0;
		};
		$scope.hasNext = function() {
			return $scope.offset + $scope.limit < $scope.total;
		};
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.limit });
		};
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.limit });
		};
		$scope.params = function() {
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/buckets/' + $scope.$parent.bucketId + '/tasks/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.tasks = response.tasks;
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t retrieve any tasks.';
					} else {
						$scope.message = 'Couldn\'t retrieve any tasks. Try again later or contact support.';
					}
				});
		};
		$scope.remove = function(taskId) {
			$scope.message = '';
			$http({ method : 'DELETE', url : '/tasks/' + taskId })
				.success(function() {
					delay($scope.refresh);
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t delete the task.';
					} else {
						$scope.message = 'Couldn\'t delete the task. Try again later or contact support.';
					}
				});
			tracker.event('action', 'delete task');
		};
		$scope.formatTooltip = function(task) {
			if (task.settings) {
				var tooltip = '';
				$.each(task.settings, function(field, value) {
					if (tooltip) {
						tooltip += '\n';
					}
					tooltip += field + ': ' + value;
				});
				return tooltip;
			} else {
				return '(no settings)';
			}
		};
	}]);

	app.controller('CreateTaskDialogController', ['$scope', '$http', 'delay', 'tracker', function($scope, $http, delay, tracker) {

		$scope.types = [
			{ id : 'beeminder', description : 'Updates a goal with event counts or value totals for each day.', url : 'https://www.beeminder.com/' },
			{ id : 'fitbark', description : 'Creates an event for the activity level for every day or hour.', url : 'https://www.fitbark.com/' },
			{ id : 'fitbit-activities', description : 'Creates an event for each activity.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-burn', description : 'Creates an event for the number of calories burned each day or hour.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-cardio', description : 'Creates an event for the daily resting heart rate, or the average hourly heart rate.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-food', description : 'Creates an event for the number of calories consumed each day.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-sleep', description : 'Creates an event for each period of sleep.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-steps', description : 'Creates an event for the number of steps each day or hour.', url : 'https://www.fitbit.com/' },
			{ id : 'fitbit-weight', description : 'Creates an event for the body weight each day.', url : 'https://www.fitbit.com/' },
			{ id : 'foursquare', description : 'Creates an event for each place visited.', url : 'https://foursquare.com/' },
			{ id : 'goodreads', description : 'Creates an event for each book read.', url : 'https://www.goodreads.com/' },
			{ id : 'google-activities', description : 'Creates an event for each activity.', url : 'https://fit.google.com/' },
			{ id : 'google-cardio', description : 'Creates an event for each heart rate measurement.', url : 'https://fit.google.com/' },
			{ id : 'google-food', description : 'Creates an event for each number of calories consumed that was recorded.', url : 'https://fit.google.com/' },
			{ id : 'google-weight', description : 'Creates an event for each body weight measurement.', url : 'https://fit.google.com/' },
			{ id : 'hexoskin-activities', description : 'Creates an event for each activity.', url : 'https://www.hexoskin.com/' },
			{ id : 'hexoskin-sleep', description : 'Creates an event for each period of sleep.', url : 'https://www.hexoskin.com/' },
			{ id : 'hipbone', description : 'Creates events for Apple Health data exported to Dropbox with the Hipbone app.', url : 'https://hipboneapp.com/' },
			{ id : 'ihealth-activities', description : 'Creates an event for each activity.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-cardio', description : 'Creates an event for each heart rate or blood pressure measurement.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-food', description : 'Creates an event for each meal.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-glucose', description : 'Creates an event for each glucose measurement.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-sleep', description : 'Creates an event for each period of sleep.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-steps', description : 'Creates an event for the number of steps logged.', url : 'https://ihealthlabs.com/' },
			{ id : 'ihealth-weight', description : 'Creates an event for each body weight measurement.', url : 'https://ihealthlabs.com/' },
			{ id : 'lastfm-tracks', description : 'Creates an event for each track played.', url : 'https://www.last.fm/' },
			{ id : 'mapmyfitness-activities', description : 'Creates an event for each activity.', url : 'https://www.mapmyfitness.com/' },
			{ id : 'mapmyfitness-sleep', description : 'Creates an event for each period of sleep.', url : 'https://www.mapmyfitness.com/' },
			{ id : 'mapmyfitness-weight', description : 'Creates an event for each body weight measurement.', url : 'https://www.mapmyfitness.com/' },
			{ id : 'misfit-activities', description : 'Creates an event for each activity.', url : 'https://misfit.com/' },
			{ id : 'misfit-sleep', description : 'Creates an event for each period of sleep.', url : 'https://misfit.com/' },
			{ id : 'misfit-steps', description : 'Creates an event for the number of steps each day.', url : 'https://misfit.com/' },
			{ id : 'netatmo', description : 'Creates events for weather station measurements.', url : 'https://www.netatmo.com/' },
			{ id : 'oura-steps', description : 'Creates an event for the number of steps and calories burned each day.', url : 'https://ouraring.com/' },
			{ id : 'oura-sleep', description : 'Creates an event for each period of sleep.', url : 'https://ouraring.com/' },
			{ id : 'oura-readiness', description : 'Creates an event for the readiness score for each day.', url : 'https://ouraring.com/' },
			{ id : 'reporter-questions', description : 'Creates an event for each question answered.', url : 'http://www.reporter-app.com/' },
			{ id : 'rescuetime-productivity', description : 'Creates an event for every hour the computer was used.', url : 'https://www.rescuetime.com/' },
			{ id : 'runkeeper-activities', description : 'Creates an event for each activity.', url : 'https://runkeeper.com/' },
			{ id : 'runkeeper-weight', description : 'Creates an event for each body weight measurement.', url : 'https://runkeeper.com/' },
			{ id : 'sleepcloud', description : 'Creates an event for each period of sleep.', url : 'https://sites.google.com/site/sleepasandroid/sleepcloud' },
			{ id : 'strava-activities', description : 'Creates an event for each activity.', url : 'https://www.strava.com/' },
			{ id : 'trakt', description : 'Creates an event for each movie or episode watched.', url : 'https://trakt.tv/' },
			{ id : 'wakatime', description : 'Creates an event for every period of time logged for a project.', url : 'https://wakatime.com/' },
			{ id : 'withings-cardio', description : 'Creates an event for each heart rate or blood pressure measurement.', url : 'https://www.withings.com/' },
			{ id : 'withings-sleep', description : 'Creates an event for each period of sleep.', url : 'https://www.withings.com/' },
			{ id : 'withings-steps', description : 'Creates an event for the number of steps each day.', url : 'https://www.withings.com/' },
			{ id : 'withings-weight', description : 'Creates an event for each body weight measurement.', url : 'https://www.withings.com/' },
			{ id : 'withings-temperature', description : 'Creates an event for each body temperature measurement.', url : 'https://www.withings.com/' }
			// { id : 'demo', description : 'Creates a single event each time this task is run.' }
		];

		function selectType(id) {
			if (id) {
				$.each($scope.types, function(i, type) {
					if (type.id === id) {
						$scope.type = type;
						return false;
					}
				});
			} else {
				$scope.type = $scope.types[0];
			}
		}
		$scope.init = function(type) {
			$scope.message = '';
			selectType(type);
			tracker.event('dialog', 'create task');
		};
		$scope.getTemplate = function(type) {
			return type ? '/' + type.id + '-settings.html' : null;
		};
		$scope.data = function() {
			return {
				type : $scope.type.id,
				bucket : $scope.bucketId,
				settings : $scope.settings
			};
		};
		$scope.create = function() {
			$scope.alert.clear();
			$http.post('/tasks/', $scope.data())
				.success(function(response, status) {
					console.assert(status === 201, status);
					$scope.closeDialog();
					delay($scope.$parent.run);
				})
				.error(function() {
					$scope.message = 'Couldn\'t create task. Try again later or contact support.';
				});
			tracker.event('action', 'create task');
		};
	}]);

	app.controller('FitBarkSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : '',
					hourly : false,
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitActivitiesSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitBurnSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'burn',
					hourly : false,
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitCardioSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'heart rate',
					hourly : false,
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitFoodSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'food',
					marker : new Date(moment().utc().subtract(1, 'years').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitSleepSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'sleep',
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitStepsSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'steps',
					hourly : false,
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitWeightSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'body',
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FoursquareSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('NetatmoSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					modules : true,
					hourly : true,
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('RunkeeperActivitiesSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					unit : 'km',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};
		$scope.getUnits = function() {
			return Field.find('distance').units;
		};

		$scope.init();
	}]);

	app.controller('RunkeeperWeightSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Body',
					unit : 'lb',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};
		$scope.getUnits = function() {
			return Field.find('weight').units;
		};

		$scope.init();
	}]);

	app.controller('StravaSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					metric : false,
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('BeeminderSettingsController', ['$scope', 'moment', 'Field', function($scope, moment, Field) {

		$scope.keyField = 'timestamp';

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					goal : null,
					filter : null,
					key_field : $scope.keyField,
					field : null,
					unit : null,
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};
		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0 ?
					$scope.settings.unit === null :
					$.inArray($scope.settings.unit, units) != -1;
		}
		function updateUnitValidity() {
			$scope.$parent.$parent.form.unit.$setValidity('unit', isUnitValid());
		}
		$scope.getFields = function() {
			return Field.findByType('numeric');
		};
		$scope.getUnits = function() {
			var f = Field.find($scope.settings.field);
			return f ? f.units : [];
		};
		$scope.subfields = $.map(Field.find($scope.keyField).subfields, function(subfield) {
			return { label : subfield, value : (subfield ? $scope.keyField + '$' + subfield : $scope.keyField) };
		});

		$scope.$watch('settings.field', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
			updateUnitValidity();
		});
		$scope.$watch('settings.unit', function() {
			updateUnitValidity();
		});
		$scope.init();
	}]);

	app.controller('HexoskinActivitiesSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Training',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('HexoskinSleepSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Sleep',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('MapMyFitnessActivitiesSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('MapMyFitnessSleepSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Sleep',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('MapMyFitnessWeightSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Body',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('HipboneSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					folder : '/Apps/Hipbone/data'
			};
		};

		$scope.init();
	}]);

	app.controller('ReporterSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					folder : 'Apps/Reporter-App'
			};
		};

		$scope.init();
	}]);

	app.controller('RescueTimeProductivitySettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {
		$scope.kinds = [
			{ 'id' : 'efficiency', 'label' : 'None' },
			{ 'id' : 'overview', 'label' : 'Category' },
			{ 'id' : 'category', 'label' : 'Sub-Category' }
		];
		$scope.sources = [
			{ 'id' : '', 'label' : 'All' },
			{ 'id' : 'computers', 'label' : 'Computers' },
			{ 'id' : 'mobile', 'label' : 'Mobile' },
			{ 'id' : 'offline', 'label' : 'Offline' }
		];
		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : '',
					kind : 'efficiency',
					source : '',
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('LastFmTracksSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'track',
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('WithingsWeightSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'body',
					unit : 'lb',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};
		$scope.getUnits = function() {
			return Field.find('weight').units;
		};

		$scope.init();
	}]);

	app.controller('WithingsSleepSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'sleep',
					marker : new Date(moment().utc().subtract(3, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('WithingsCardioSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'heart rate',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('WithingsStepsSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'steps',
					unit : 'mi',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};
		$scope.getUnits = function() {
			return Field.find('distance').units;
		};

		$scope.init();
	}]);

	app.controller('WithingsTemperatureSettingsController', ['$scope', '$http', 'Field', 'moment', function($scope, $http, Field, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
				tag : 'body',
				unit : 'C',
				marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
				timezone : 'UTC'
			};
		};
		$scope.getUnits = function() {
			return Field.find('temperature').units;
		};

		$scope.init();
	}]);

	app.controller('SleepCloudSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'sleep'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthActivitiesSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Activity',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthCardioSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Heart',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthFoodSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Meal',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthGlucoseSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Glucose',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthSleepSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Sleep',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthStepsSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Steps',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('IHealthWeightSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Body',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('MisfitActivitiesSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('MisfitSleepSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Sleep',
					marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('MisfitStepsSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Steps',
					marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('OuraStepsSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
				tag : 'Steps',
				marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('OuraSleepSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
				tag : 'Sleep',
				marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('OuraReadinessSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
				tag : 'Readiness',
				marker : new Date(moment().utc().subtract(6, 'months').startOf('month').valueOf()),
				timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('GoodreadsSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
				tag : 'Book',
				shelf : 'read',
				marker : new Date(moment('2007-01-01').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('GoogleFitActivitiesSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					derived : false,
					metric : true,
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('GoogleFitCardioSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Heart Rate',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('GoogleFitFoodSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Food',
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('GoogleFitWeightSettingsController', ['$scope', '$http', 'moment', function($scope, $http, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'Weight',
					metric : false,
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
					timezone : 'UTC'
			};
		};

		$scope.init();
	}]);

	app.controller('TraktSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract(12, 'months').startOf('month').valueOf()),
			};
		};

		$scope.init();
	}]);

	app.controller('WakaTimeSettingsController', ['$scope', 'moment', function($scope, moment) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'project',
					marker : new Date(moment().utc().subtract(2, 'weeks').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('DemoSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'demo'
			};
		};

		$scope.init();
	}]);

	app.controller('CredentialsController', ['$scope', '$http', '$routeParams', '$location', '$window', 'localStorage', function($scope, $http, $routeParams, $location, $window, localStorage) {

		$scope.credentialsId = $routeParams.credentialsId;
		if ($scope.credentialsId === '-') {
			$scope.credentialsId = localStorage.getItem('credentials');
		}

		$http.post('/credentials/' + $scope.credentialsId, { 'credentials' : $location.search() })
			.success(function() {
				$scope.alert.show('Updated credentials.', 'alert-success');
				localStorage.removeItem('credentials');
				if ($window.opener) {
					$window.opener.angular.element('#app').scope().$broadcast('credentials');
					$window.close();
				}
			})
			.error(function(response, status) {
				if (status < 500) {
					$scope.message = 'Can\'t update credentials.';
				} else {
					$scope.message = 'Couldn\'t update credentials. Try again later or contact support.';
				}
			});
	}]);

	app.controller('DocumentController', ['$scope', '$location', '$routeParams', '$timeout', function($scope, $location, $routeParams, $timeout) {
		if ($routeParams.section) {
			var id = $location.path().substring(1).replace('/', '-');
			var element = document.getElementById(id);
			if (element) {
				$timeout(function() {
					element.scrollIntoView(true);
				});
			}
		}
	}]);

	app.controller('PricingController', ['$scope', '$http', function($scope, $http) {

		$scope.$watch('user', function(user) {
			if (user) {
				$http.get('/users/' + user['@id'] + '/quota')
					.success(function(response) {
						$scope.quota = response;
					})
					.error(function() {
						$scope.quota = null;
					});
			}
		});
	}]);

	app.controller('FreePlanDialogController', ['$scope', '$http', '$location', 'tracker', function($scope, $http, $location, tracker) {

		$scope.init = function() {
			$scope.message = '';
			$scope.processing = false;
			tracker.event('dialog', 'cancel payment');
		};

		$scope.cancel = function() {
			$scope.processing = true;
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/users/' + $scope.user['@id'] + '/payment' })
				.success(function() {
					$scope.processing = false;
					$scope.closeDialog();
					$scope.whoami();
				})
				.error(function() {
					$scope.processing = false;
					$scope.message = 'Couldn\'t change the plan. Try again later or contact support.';
				});
			tracker.event('action', 'payment cancelled');
		};
	}]);

	app.controller('PersonalPlanDialogController', ['$scope', '$http', '$location', 'braintree', 'merchantId', 'tracker', function($scope, $http, $location, braintree, merchantId, tracker) {

		$scope.merchantId = merchantId;
		$scope.integration = null;

		$scope.init = function() {
			$scope.processing = false;
			$scope.message = '';
			$http.post('/payments/token').success(function(response) {
				braintree.setup(response.value, 'dropin', {
					container : 'braintree',
					onReady : function(integration) {
						$scope.integration = integration;
					},
					onPaymentMethodReceived : function(payment) {
						pay(payment.nonce);
					},
					onError : function(error) {
						$scope.message = 'Couldn\'t save the payment method. Try again later or contact support.';
					}
				});
			});
			tracker.event('dialog', 'payment');
		};

		function pay(nonce) {
			$scope.processing = true;
			$scope.message = '';
			$scope.alert.clear();
			$http.post('/payments/', { 'price' : 5.0, 'nonce' : nonce })
				.success(function() {
					$scope.processing = false;
					$scope.closeDialog();
					$scope.whoami();
				})
				.error(function(response, status) {
					$scope.processing = false;
					if (status < 500) {
						$scope.message = 'Can\'t process the payment.';
					} else {
						$scope.message = 'Couldn\'t process the payment. Try again later or contact support.';
					}
				});
			tracker.event('action', 'payment');
		}

		$scope.close = function() {
			if ($scope.integration) {
				$scope.integration.teardown();
			}
		};

	}]);

	app.factory('Field', ['User', 'moment', function(User, moment) {

		var fields = [];
		var fieldsByName = {};

		var Field = function(name, icon, type, units, readOnly, toText, toHtml, toNumber, formatAxis, minValue, maxValue, subfields) {
			this.name = name;
			this.icon = icon;
			this.type = type;
			this.units = units;
			this.readOnly = readOnly;
			this.toText = toText;
			this.toHtml = toHtml;
			this.toNumber = toNumber;
			this.formatAxis = formatAxis;
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.subfields = subfields;
		};

		var toNumber = function(value) {
			if (value === null) {
				return null;
			}
			if (typeof value === 'number') {
				return value;
			}
			if (typeof value === 'string') {
				return Number(value);
			}
			if (typeof value === 'object' && value.hasOwnProperty('@value')) {
				return value['@value'];
			}
			return Number.NaN;
		};

		Field.find = function(name) {
			return fieldsByName[name];
		};

		Field.findAll = function() {
			return fields;
		};

		Field.findEditable = function() {
			return $.grep(fields, function(field) {
				return !field.readOnly;
			});
		};

		Field.findByType = function(type) {
			return $.grep(fields, function(field) {
				return field.type === type;
			}).sort(function(a, b) {
				return a.name > b.name ? 1 : -1;
			});
		};

		function encode(value) {
			return $('<div />').text(value).html();
		}

		function register(fieldOptions) {
			console.assert(fieldOptions.name, 'missing <name>');
			var field = new Field(
				fieldOptions.name,
				fieldOptions.icon || '',
				fieldOptions.type || 'numeric',
				fieldOptions.units || [],
				fieldOptions.readOnly === true,
				fieldOptions.toText || function(value) { return value; },
				fieldOptions.toHtml || function(value) { return value; },
				fieldOptions.toNumber || toNumber,
				fieldOptions.formatAxis || function() { },
				fieldOptions.minValue,
				fieldOptions.maxValue,
				fieldOptions.subfields
			);
			fields.push(field);
			fieldsByName[field.name] = field;
		}

		register({
			name : 'tag',
			icon : 'fa-tag',
			type : 'text',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Tag"></i> ' + encode(value) +
				'</span>';
			}
		});

		register({
			name : 'resource',
			icon : 'fa-bookmark',
			type : 'object',
			toHtml : function(value) {
				return '<span>' +
					'<i class="fa ' + this.icon + '" title="Resource"></i>&nbsp;' +
					'<a href="/to?url=' + encode(value.url) + '" target="_blank" rel="nofollow noopener noreferrer">' + encode(value.title) + '</a>' +
				'</span>';
			}
		});

		register({
			name : 'distance',
			icon : 'fa-arrows-h',
			type : 'numeric',
			units : [ 'mi', 'yd', 'ft', 'in', 'km', 'm', 'cm', 'mm' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Distance"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'height',
			icon : 'fa-arrows-v',
			type : 'numeric',
			units : [ 'mi', 'ft', 'in', 'km', 'm', 'cm', 'mm' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Height"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'weight',
			icon : 'fa-caret-square-o-down',
			type : 'numeric',
			units : [ 'lb', 'oz', 'kg', 'g', 'mg', 'ug', 'ng', 'st' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Weight"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'percentage',
			icon : 'fa-th',
			type : 'numeric',
			toText : function(value) {
				return value + '%';
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Percentage"></i> <abbr title="' + value + '%">' + Math.round(value) + '%</abbr>' +
				'</span>';
			},
			minValue : 0,
			maxValue : 100
		});

		register({
			name : 'moon',
			icon : 'fa-moon-o',
			type : 'numeric',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Moon"></i> ' + value + '%' +
				'</span>';
			},
			minValue : 0,
			maxValue : 100
		});

		register({
			name : 'volume',
			icon : 'fa-flask',
			type : 'numeric',
			units : [ 'L', 'dL', 'cL', 'mL', 'gal', 'qt', 'pt', 'cups', 'fl_oz' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Volume"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'concentration',
			icon : 'fa-tint',
			type : 'numeric',
			units : [ 'g/L', 'mg/L', 'ug/L', 'ng/L', 'g/dL', 'mg/dL', 'ug/dL', 'ng/dL', 'g/mL', 'mg/mL', 'ug/mL', 'ng/mL' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Volume"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'distance/volume',
			icon : 'fa-flask',
			type : 'numeric',
			units : [ 'mpg', 'kpl' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Distance/Volume"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'humidity',
			icon : 'fa-tint',
			type : 'numeric',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Humidity"></i> ' + value + '%' +
				'</span>';
			}
		});

		register({
			name : 'pressure',
			icon : 'fa-arrows-alt',
			type : 'numeric',
			units : [ 'Pa', 'hPa', 'kPa', 'mbar', 'bar', 'mmHg', 'inHg', 'psi', 'cm_wg' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Pressure"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'sound',
			icon : 'fa-volume-up',
			type : 'numeric',
			units : [ 'dB' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Sound Level"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'location',
			icon : 'fa-map-marker',
			type : 'object',
			toText : function(value) {
				return typeof value === 'object' ? encode(Math.round(value.lat * 1000) / 1000 + ', ' + Math.round(value.lon * 1000) / 1000) : '';
			},
			toHtml : function(value, interactive) {
				if (interactive) {
					var ngClick = "addConstraint('location', '" + this.toText(value).replace(' ', '') + '~100 m' + "', false)";
					return '<span class="nowrap">' +
						'<i class="fa ' + this.icon + '" title="Location"></i> ' +
						'<a data-ng-click="' + ngClick + '">' + this.toText(value) + '</a>' +
					'</span>';
				} else {
					return '<span class="nowrap">' +
						'<i class="fa ' + this.icon + '" title="Location"></i> ' + this.toText(value) +
					'</span>';
				}
			}
		});

		register({
			name : 'timestamp',
			icon : 'fa-calendar-o',
			type : 'object',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Timestamp"></i> ' +
					'<abbr title="' + value + '">' + moment(value).utcOffset(value).fromNowOrNow(false) + '</abbr>' +
				'</span>';
			},
			subfields : [ '', 'min', 'max' ]
		});

		register({
			name : 'velocity',
			icon : 'fa-tachometer',
			type : 'numeric',
			units : [ 'm/s', 'mph', 'kmh', 'kn', 'Mach' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Velocity"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'pace',
			icon : 'fa-clock-o',
			type : 'numeric',
			units : [ 's/km', 's/mi' ],
			toText : function(value) {
				return typeof value === 'object' ? moment.duration(value['@value'], 'seconds').countdownCompact() + '/' + value.unit.substring(2) : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Pace"></i> ' + this.toText(value) +
				'</span>';
			},
			formatAxis : function(options) {
				options.type = 'datetime';
				options.labels = {
					autoRotation : false,
					formatter : function() {
						return this.value !== 0 ? moment.duration(this.value, 'seconds').countdownCompact() : '0';
					}
				};
			},
			minValue : 0
		});

		register({
			name : 'duration',
			icon : 'fa-clock-o',
			type : 'numeric',
			toText : function(value) {
				return value ? moment.duration(toNumber(value)).countdown(2) : 0;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Duration"></i> ' +
					'<abbr title="' + moment.duration(value).toISOString() + '">' + this.toText(value) + '</abbr>' +
				'</span>';
			},
			toNumber : function(value) {
				var n = toNumber(value);
				if (value && isNaN(n)) {
					$.each(value.split(' '), function(i, token) {
						var m = /^(-?\d+)(d|h|min|s)?$/.exec(token);
						if (m) {
							var ms = Number(m[1]);
							switch (m[2]) {
								case 'd':
									ms *= 24;
									/* falls through */
								case 'h':
									ms *= 60;
									/* falls through */
								case 'min':
									ms *= 60;
									/* falls through */
								case 's':
									ms *= 1000;
									/* falls through */
							}
							n = isNaN(n) ? ms : n + ms;
						} else {
							n = Number.NaN;
							return false;
						}
					});
				}
				return n;
			},
			formatAxis : function(options) {
				options.type = 'datetime';
				options.labels = {
					autoRotation : false,
					formatter : function() {
						return this.value !== 0 ? moment.duration(this.value).countdown(2) : '0';
					}
				};
			},
			minValue : 0
		});

		register({
			name : 'frequency',
			icon : 'fa-heart',
			type : 'numeric',
			units : [ 'bpm', 'rpm', 'Hz' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Frequency"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'bits',
			icon : 'fa-hdd-o',
			type : 'numeric',
			units : [ 'bit', 'B', 'KB', 'MB', 'GB', 'TB', 'PB', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Bits"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'count',
			icon : 'fa-th',
			type : 'numeric',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Count"></i> ' + value.toLocaleString() +
				'</span>';
			},
			minValue : 0
		});

		register({
			name : 'energy',
			icon : 'fa-fire',
			type : 'numeric',
			units : [ 'J', 'kJ', 'cal', 'kcal', 'kWh' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Energy"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'light',
			icon : 'fa-sun-o',
			type : 'numeric',
			units : [ 'lx' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Light"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'temperature',
			icon : 'fa-fire',
			type : 'numeric',
			units : [ 'C', 'F', 'K' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Temperature"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'rating',
			icon : 'fa-star',
			type : 'numeric',
			toText : function(value) {
				return value + '%';
			},
			toHtml : function(value) {
				var stars = Math.round((value || 0) / 20);
				var html = '<span class="nowrap" title="' + this.toText(value) + '">';
				for (var i = 0; i < 5; ++i) {
					html += '<i class="fa ' + (stars > i ? 'fa-star' : 'fa-star-o') + '"></i>';
				}
				html += '</span>';
				return html;
			},
			minValue : 0,
			maxValue : 100
		});

		register({
			name : 'currency',
			icon : 'fa-money',
			type : 'numeric',
			toText : function(value) {
				return value.toFixed(2);
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Currency"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'note',
			icon : 'fa-comment-o',
			type : 'object',
			toHtml : function(value) {
				return '<span>' +
					'<i class="fa ' + this.icon + '" title="Note"></i>&nbsp;' + encode(value) +
				'</span>';
			}
		});

		register({
			name : 'author',
			icon : 'fa-user',
			type : 'text',
			readOnly : true,
			toText : function(value) {
				return User.find(value).getName();
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="User"></i> ' + this.toText(value) +
				'</span>';
			}
		});

		register({
			name : 'source',
			icon : 'fa-external-link',
			type : 'object',
			readOnly : true,
			toText : function(value) {
				return value.title;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="fa ' + this.icon + '" title="Source"></i> <a href="/to?url=' + encode(value.url) + '" target="_blank" rel="nofollow">' + encode(value.title) + '</a>' +
				'</span>';
			}
		});

		return Field;
	}]);

	app.directive('uiBindEvent', ['Field', '$compile', function(Field, $compile) {
		return {
			restrict : 'A',
			link : function(scope, element, attrs) {
				var event = scope.$eval(attrs.uiBindEvent);
				var html = '';
				if (event) {
					var count = 0;
					$.each(Field.findAll(), function(i, field) {
						var value = event[field.name];
						if (angular.isDefined(value)) {
							$.each($.isArray(value) ? value : [ value ], function(i, value) {
								if (count > 0) {
									html += ' &nbsp; ';
								}
								html += field.toHtml(value, true);
								++count;
							});
						}
					});
				}
				element.html(html);
				$compile(element.contents())(scope);
			}
		};
	}]);

	app.filter('field', ['Field', function(Field) {
		return function(value, fieldName) {
			var field = Field.find(fieldName);
			console.assert(field, 'Don\'t know how to format field: ' + fieldName);
			return field.toHtml(value);
		};
	}]);

	app.filter('age', [ 'moment', function(moment) {
		return function(date) {
			return date ? moment(date).utcOffset(date).fromNowOrNow(true) : '';
		};
	}]);

	app.filter('duration', ['moment', function(moment) {
		return function(millis) {
			return isFinite(millis) ? moment.duration(millis).countdown(1) : '';
		};
	}]);

	app.filter('stars', ['Field', function(Field) {
		var field = Field.find('rating');
		return function(rating) {
			return field.toHtml(rating);
		};
	}]);

	app.filter('username', ['User', function(User) {
		return function(identity) {
			return identity ? User.find(identity).getName() : '';
		};
	}]);

	app.filter('startFrom', function() {
		return function(input, start) {
			if (input) {
				return input.slice(+start);
			}
		};
	});

	app.factory('unauthorizedInterceptor', ['$q', '$rootScope', function($q, $rootScope) {
		return {
			response : function(response) {
				return response;
			},
			responseError : function(response) {
				if (response.status === 401) {
					$rootScope.$broadcast('event:unauthorized');
				}
				return $q.reject(response);
			}
		};
	}]);

	app.config(['$httpProvider', function($httpProvider) {
		$httpProvider.interceptors.push('unauthorizedInterceptor');
	}]);

	app.directive('uiQuota', ['$interpolate', '$filter', function($interpolate, $filter) {
		return {
			restrict : 'A',
			compile : function() {
				return function(scope, element, attrs) {
					var template = $interpolate(
						'<div class="progress" title="{{title}}">' +
						'  <div class="bar {{class}}" style="width:{{percent}}%;"></div>' +
						'</div>');
					scope.$watch(attrs.uiQuota, function(quota) {
						if (quota) {
							var percent = Math.max(Math.ceil(quota.remaining / quota.limit * 100), 1);
							element.html(template({
								'title' : 'You have used ' + $filter('number')(quota.used) + '/' + $filter('number')(quota.limit) + ' events this month.',
								'class' : percent > 10 ? 'bar-success' : percent > 1 ? 'bar-warning' : 'bar-danger',
								'percent' : percent
							}));
						}
					});
				};
			}
		};
	}]);

	app.directive('uiCurrentYear', function() {
		return {
			restrict : 'A',
			link : function(scope, element) {
				element.html(new Date().getFullYear());
			}
		};
	});

	app.directive('uiFocusOn', function() {
		return {
			restrict : 'A',
			link : function(scope, element, attrs) {
				var tokens = attrs.uiFocusOn.split(':', 2);
				console.assert(tokens.length === 2);
				scope.$on(tokens[0], function(event, param) {
					if (event.name === tokens[0] && param === tokens[1]) {
						setTimeout(function() {
							element.select();
						}, 0);
					}
				});
			}
		};
	});

	app.directive('uiModal', function() {
		return {
			restrict : 'A',
			link : function(scope, element, attrs) {
				var id = attrs.id || scope.$eval(attrs.uiModal);
				console.assert(id, '@id is required');
				element.addClass('modal hide');
				element.on('hidden', function() {
					scope.closeDialog();
					if (scope.close) {
						scope.close();
					}
				});
				scope.$on('openDialog', function(event, dialogId, param) {
					if (dialogId === id) {
						if (scope.init) {
							scope.init(param);
						}
						element.modal('show');
					} else {
						element.modal('hide');
					}
				});
			}
		};
	});

	app.directive('uiDatepicker', ['moment', function(moment) {
		return {
			require : '?ngModel',
			restrict : 'A',
			link : function($scope, element, attrs, controller) {
				var updateModel = function(event) {
					element.datepicker('hide');
					element.blur();
					return $scope.$apply(function() {
						return controller.$setViewValue(event.date);
					});
				};
				if (controller !== null) {
					controller.$formatters.unshift(function(value) {
						return typeof value === 'object' ? moment.utc(value).format('YYYY-MM-DD') : null;
					});
					controller.$render = function() {
						element.datepicker().data().datepicker.date = controller.$modelValue;
						element.datepicker('setValue');
						element.datepicker('update');
						return controller.$viewValue;
					};
				}
				var options = {
					format : 'yyyy-mm-dd',
					weekStart : 1
				};
				return element.datepicker(options).on('changeDate', updateModel);
			}
		};
	}]);

	app.directive('uiTimepicker', ['moment', function(moment) {
		return {
			require: 'ngModel',
			link: function(scope, element, attrs, controller) {
				controller.$parsers.push(function(s) {
					return moment(s, [ 'H:mm:ss', 'H:mm:ss.SSS' ], true);
				});
				controller.$formatters.push(function(time) {
					return time.format('HH:mm:ss');
				});
			}
		};
	}]);

	app.directive('uiDefer', ['$timeout', function($timeout) {
		return {
			require : 'ngModel',
			link : function($scope, $element, $attrs, modelCtrl) {
				var $setViewValue = modelCtrl.$setViewValue;
				var bufferedValue;
				modelCtrl.$setViewValue = function(value) {
					bufferedValue = value;
				};
				$element.bind('change', function() {
					$timeout(function() {
						$setViewValue.call(modelCtrl, bufferedValue);
					});
				});
			}
		};
	}]);

	app.directive('uiChartOptions', ['tracker', function(tracker) {
		return {
			restrict : 'A',
			scope : true,
			link : function(scope, element, attrs) {
				var defaultOptions = {
					chart : {
						renderTo : element[0]
					},
					exporting : {
						enabled : false
					}
				};
				if (attrs.uiSnapshot !== undefined) {
					scope.$on('snapshot', function() {
						if (scope.chart) {
							var filename = scope.settings.label.replace(/\s+/g, '-').toLowerCase();
							scope.chart.exportChart({
								filename : filename,
								type : 'image/png',
								sourceWidth : element[0].offsetWidth,
								sourceHeight : element[0].offsetHeight,
								scale : 1,
								url : 'https://export.highcharts.com/'
							});
							tracker.event('action', 'snapshot');
						}
					});
				}
				scope.$watch(attrs.uiChartOptions, function(newOptions, oldOptions) {
					if (!angular.equals(newOptions, oldOptions)) {
						if (oldOptions) {
							scope.chart.destroy();
						}
						if (newOptions) {
							scope.chart = new Highcharts.Chart($.extend(true, {}, newOptions, defaultOptions));
							$('#' + attrs.uiId + '-tab').on('shown', function() {
								scope.chart.reflow();
							});
							if (newOptions.playable) {
								scope.$on('tic', function(event, clock, active) {
									var data = scope.chart.series[0].data;
									data[clock % data.length].select(active);
								});
							}
						}
					}
				}, true);
			}
		};
	}]);

	app.directive('uiBucketLabel', ['Bucket', function(Bucket) {
		return {
			restrict : 'A',
			link : function(scope, element, attrs) {
				var id = scope.$eval(attrs.uiBucketLabel);
				element.html(id);
				Bucket.getLabel(id, function(label) {
					element.html(label);
				});
			}
		};
	}]);

	app.directive('uiFieldValue', function() {
		return {
			require: 'ngModel',
			link: function(scope, element, attrs, controller) {
				controller.$parsers.unshift(function(value) {
					var field = scope.getField();
					var n = field.toNumber(value);
					return !isNaN(n) ? n : value;
				});
				controller.$formatters.unshift(function(value) {
					var field = scope.getField();
					return field.toText(value);
				});
			}
		};
	});

	app.directive('uiPasswordMatch', [function() {
		return {
			require : 'ngModel',
			link : function(scope, element, attrs, controller) {
				var firstPassword = '#' + attrs.uiPasswordMatch;
				element.add(firstPassword).on('keyup', function() {
					scope.$apply(function() {
						var v = element.val() === $(firstPassword).val();
						controller.$setValidity('match', v);
					});
				});
			}
		};
	}]);

	app.directive('uiCheckFilter', ['$http', function($http) {

		var validationErrorKey = 'filter';

		function checkSyntax(value) {
			return !value || $.grep(value.split('|'), function(expression) {
				return expression.split(':').length != 2;
			}).length === 0;
		}

		function checkResults(bucket, value, callback) {
			$http.get('/buckets/' + bucket['@id'] + '/?' + $.param({ 'q' : value.split('|'), 'limit' : 0 }, true))
			.success(function() {
				callback(true);
			})
			.error(function() {
				callback(false);
			});
		}

		return {
			require : 'ngModel',
			link : function(scope, element, attrs, controller) {
				if (!controller.$options) {
					controller.$options = {
						updateOn : 'blur',
						debounce : {
							'default' : 1000,
							'blur' : 0
						}
					};
					controller.$options.updateOnDefault = true;
				}
				controller.$parsers.unshift(function(value) {
					var valid = checkSyntax(value);
					if (valid && value) {
						var bucket = scope.$eval(attrs.uiCheckFilter);
						checkResults(bucket, value, function(valid) {
							controller.$setValidity(validationErrorKey, valid);
						});
					} else {
						controller.$setValidity(validationErrorKey, valid);
					}
					return value;
				});
				controller.$formatters.unshift(function(value) {
					var valid = checkSyntax(value);
					controller.$setValidity(validationErrorKey, valid);
					return value;
				});
			}
		};
	}]);

	/* Based on http://blog.parkji.co.uk/2013/08/11/native-drag-and-drop-in-angularjs.html */
	app.directive('uiDraggable', function() {
		return function(scope, element, attrs) {
			var el = element[0];
			el.draggable = true;
			el.addEventListener('dragstart', function(e) {
					e.dataTransfer.effectAllowed = 'move';
					e.dataTransfer.setData('text', attrs.uiDraggable);
					this.classList.add('drag');
					return false;
				}, false);
			el.addEventListener('dragend', function() {
					this.classList.remove('drag');
					return false;
				}, false);
		};
	});

	app.directive('uiDroppable', ['$timeout', function($timeout) {

		return {
			link : function(scope, element, attrs) {
				var el = element[0];
				el.addEventListener('dragover', function(e) {
						e.dataTransfer.dropEffect = 'move';
						if (e.preventDefault) {
							e.preventDefault();
						}
						this.classList.add('drop');
						return false;
					}, false);

				el.addEventListener('dragenter', function(e) {
						if (e.preventDefault) {
							e.preventDefault();
						}
						this.classList.add('drop');
						return false;
					}, false);

				el.addEventListener('dragleave', function() {
						this.classList.remove('drop');
						return false;
					}, false);

				el.addEventListener('drop', function(e) {
						if (e.stopPropagation) {
							e.stopPropagation();
						}
						if (e.preventDefault) {
							e.preventDefault();
						}
						this.classList.remove('drop');
						scope.$apply(function(scope) {
							var sourceId = e.dataTransfer.getData('text');
							var targetId = attrs.uiDroppable;
							if (sourceId !== targetId) {
								scope.moveWidget(sourceId, targetId);
								$timeout(function() {
									$('#' + sourceId + '-tab').tab('show');
									scope.setDirty(true);
								}, 0);
							}
						});
						return false;
					}, false);
			}
		};
	}]);

	app.directive('uiBlurOnEnter', function() {
		return {
			link : function(scope, element) {
				var el = element[0];
				el.addEventListener('keydown', function(e) {
					if (e.keyCode === 13) {
						el.blur();
					}
				});
			}
		};
	});

	app.directive('uiHighlight', ['$window', function($window) {
		return {
			link : function(scope, element) {
				$window.hljs.highlightBlock(element[0]);
			}
		};
	}]);

	app.directive('uiDownloadCsv', ['$window', function($window) {
		return {
			link : function(scope, element) {
				element.bind('click', function() {
					var spreadsheet = scope.toSpreadsheet();
					var url = ($window.URL || $window.webkitURL).createObjectURL(spreadsheet.toBlob());
					var a = document.createElement('a');
					document.body.appendChild(a);
					a.style.display = 'none';
					a.download = scope.settings.label.replace(/\s+/g, '-').toLowerCase() + '.csv';
					a.href = url;
					a.click();
					document.body.removeChild(a);
					$window.URL.revokeObjectURL(url);
				});
			}
		};
	}]);

	app.directive('uiTimezoneSelect', ['$http', function($http) {
		return {
			link : function(scope) {
				scope.timezones = Intl.supportedValuesOf('timeZone');
				scope.settings.timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
			}
		};
	}]);

}());
