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

	var app = angular.module('ZenoModule', [ 'ngRoute', 'ngSanitize' ]);

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
		}

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
		}

		return moment;
	});

	app.factory('token', ['$http', 'localStorage', function($http, localStorage) {
		var key = 'access_token';
		var get = function() {
			return localStorage.getItem(key);
		};
		var set = function(token) {
			if (token) {
				localStorage.setItem(key, token)
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
		}
	}]);

	app.factory('tracker', function() {
		return {
			event : function(category, action, label) {
				_gaq.push([ '_trackEvent', category, action, label ]);
			},
			timing : function(category, action, time, label) {
				_gaq.push([ '_trackTiming', category, action, time, label, 100 ]);				
			},
			pageview : function(url) {
				_gaq.push([ '_trackPageview', url ]);
			},
			variable : function(index, name, value, scope) {
				_gaq.push([ '_setCustomVar', index, name, value, scope ]);
			}
		};
	});

	app.constant('googleApiKey', 'AIzaSyDv7t1arxF_85-QF-ZUi9C4MV1z94BsH0I');

	app.constant('timezone', moment().format('Z'));

	// TODO should inject this, but can't inject into config...
	var cacheBuster = function() {
		var version = function() {
			var meta = document.getElementsByTagName('meta');
			for (var i = 0; i < meta.length; ++i) {
				if (meta[i].getAttribute('property') == 'version') {
					return meta[i].content;
				}
			}
			throw new Error('missing version');
		}();
		return {
			rewrite : function(path) {
				return path.replace(/\.(.+)$/, '-' + version + '.$1');
			}
		}
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
		$rootScope.$on('$routeChangeSuccess', function(event, current, previous) {
			if (current.$$route) {
				$rootScope.page.setTitle(current.$$route.title);
			}
		});
	}]);

	app.controller('ApplicationController', ['$scope', '$route', '$http', '$location', 'Alert', 'User', 'token', 'tracker', 'delay', function($scope, $route, $http, $location, Alert, User, token, tracker, delay) {

		$scope.alert = new Alert();

		$scope.whoami = function(success) {
			$http.get('/who').success(function(response) {
				$scope.user = response ? new User(response) : null;
				if ($scope.user) {
					if (success) {
						success($scope.user);
					}
					tracker.variable(1, 'user type', $scope.user.name ? 'registered' : 'unregistered', 1);
				}
			});
		};
		$scope.undo = function(commandId) {
			$scope.alert.clear();
			$http.post('/journal/' , { 'undo' : commandId })
				.success(function(response, status) {
					delay($route.reload);
				})
				.error(function(response) {
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
				.success(function(response) {
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
		$scope.closeDialog = function() {
			$scope.openDialog(null);
		};

		$scope.$on('$routeChangeStart', function() {
			$scope.alert.clear();
		});
		$scope.$on('$routeChangeSuccess', function() {
			tracker.pageview($location.path());
			tracker.event('page', $location.path());
		});
		$scope.whoami();
	}]);

	app.factory('Alert', function() {

		var Alert = function() {
			this.clear();
		}

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
		}

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

		var separator = ':';

		var Constraint = function(field, value, negated) {
			this.field = field;
			this.value = value;
			this.negated = negated;
		}

		Constraint.prototype.toString = function() {
			return (this.negated ? '-' : '') + this.field + separator + this.value;
		};

		Constraint.parse = function(s) {
			var negated = false;
			if (s.length > 1 && s.charAt(0) == '-') {
				negated = true;
				s = s.substring(1);
			}
			var pos = s.indexOf(separator);
			if (pos < 1 || pos > s.length - 1) {
				throw 'Can\'t parse constraint: ' + s;
			}
			var field = s.substring(0, pos);
			var value = s.substring(pos + 1);
			return new Constraint(field, value, negated);
		}

		return Constraint;
	});

	app.controller('UserController', ['$scope', '$http', '$routeParams', 'User', 'tracker', function($scope, $http, $routeParams, User, tracker) {

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
				} else {
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
					});
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
		$scope.close = function() {
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

	app.controller('SignInDialogController', ['$scope', '$http', '$location', '$route', 'User', 'token', 'tracker', function($scope, $http, $location, $route, User, token, tracker) {

		$scope.init = function() {
			$scope.message = '';
			$("#sign-in-username").val('');
			$("#sign-in-password").val('');
			tracker.event('dialog', 'sign in');
		};
		$scope.signIn = function() {
			// autocompleted values don't propagate to model!
			var username = $("#sign-in-username").val();
			var password = $("#sign-in-password").val();
			$http({ method: 'POST', url: '/oauth/token', 
				data: $.param({ 'grant_type' : 'password', 'username' : username, 'password' : password }),
				headers: { 'Content-Type' : 'application/x-www-form-urlencoded' }
			})
				.success(function(response) {
					console.assert(response.access_token, 'missing token in sign in response');
					token.set(response.access_token);
					$scope.closeDialog();
					$scope.whoami();
					if ($location.url() === '/') {
						$location.url('/users/' + username);
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
		}

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

	app.controller('SignUpDialogController', ['$scope', '$http', '$location', 'User', 'tracker', function($scope, $http, $location, User, tracker) {

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
			if ($scope.password !== $scope.passwordConfirmed) {
				$scope.message = 'Passwords don\'t match.';
				return;
			}
			$http.post('/users/', $scope.data())
				.success(function(response) {
					$scope.$parent.user = new User(response);
					$scope.closeDialog();
					$location.url('/users/' + $scope.$parent.user.name);
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

		$scope.client = $location.search()['client_id']
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
				order : 'label', 
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/users/' + $scope.profile['@id'] + '/buckets/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.buckets = response.buckets;
				});
		};
		$scope.run = function(bucketId) {
			$scope.alert.clear();
			taskRunner.runAll($scope, bucketId, function() {
				delay($scope.refresh);
			});
			tracker.event('action', 'run tasks');
		};
		$scope.remove = function(bucketId) {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/buckets/' + bucketId })
				.success(function(response, status, headers) {
					$scope.alert.show('Deleted a bucket.', 'alert-success', headers('X-Command-ID'));
					$scope.offset = 0;
					$scope.refresh();
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Can\'t delete the bucket.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t delete the bucket. Try again later or contact support.', 'alert-error');
					}
				});
			tracker.event('action', 'delete bucket');
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
			$scope.template = null;
			$http.get(cacheBuster.rewrite('/dashboard/templates.json')).success(function(response) {
				$scope.templates = response;
				$scope.template = $scope.templates[0];
			});
			tracker.event('dialog', 'create bucket');
		};
		$scope.valid = function() {
			return true;
		};
		$scope.validLabel = function() {
			return $scope.label && $scope.label.length > 0;
		};
		$scope.create = function() {
			$scope.alert.clear();
			$http.post('/buckets/', { label : $scope.label, widgets : $scope.template.widgets })
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.closeDialog();
					$location.url(location);
					if ($scope.template.task) {
						$timeout(function() {
							$scope.openDialog('create-task-dialog', $scope.template.task);
						}, 500);
					}
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create bucket.';					
					} else {
						$scope.message = 'Couldn\'t create bucket. Please try agan later or contact support.';					
					}
				});
			tracker.event('action', 'create bucket');
		};
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
					}).length == 0;
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
				var id = '';
				var pos;
				for (var i = 0; i < len; ++i) {
					pos = Math.floor(Math.random() * chars.length);
					id += chars.substring(pos, pos + 1);
				}
				return id;
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
      	settings : { limit : 10, order : 'timestamp', reverse : false },
      	singleton : true
      },
      {
      	type : 'count',
      	label : 'Count', 
      	description : 'Counts events by tag or author.',
      	thumbnail : cacheBuster.rewrite('/img/widgets/count.png'),
      	settings : { field : 'tag', order : 'count', reverse : false, limit : 5 }
      },
      {
      	type : 'map',
      	label : 'Map', 
      	description : 'Shows event locations on a map.',
      	thumbnail : cacheBuster.rewrite('/img/widgets/map.png'),
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
      	settings : { key_field : 'author', value_field : 'distance', unit : 'mi', order : 'total', limit : 10 }
      },                    
	  	{
      	type : 'gantt',
      	label : 'Frequency', 
      	description : 'Shows how long ago and how often certain events occur.',
      	thumbnail : cacheBuster.rewrite('/img/widgets/gantt.png'),
      	settings : { field : 'tag', order : 'max', limit : 10 }
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
		}

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

	app.controller('DashboardController', ['$scope', '$http', '$route', '$routeParams', '$location', '$q', '$window', 'Bucket', 'Field', 'Constraint', 'tracker', 'delay', 'token', 'taskRunner', function($scope, $http, $route, $routeParams, $location, $q, $window, Bucket, Field, Constraint, tracker, delay, token, taskRunner) {

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
		$scope.getTemplate = function(type) {
			return cacheBuster.rewrite('/dashboard/' + type + '.html');
		};
		$scope.register = function(widget) {
			$scope.widgets.push(widget);
			// TODO find better logic to ensure that all widgets are present before refreshing
			if ($scope.widgets.length >= $scope.bucket.widgets.length) {
				$scope.refresh();
			}
		};
	
		function search(q, facets) {
			return $http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'facet' : facets }, true))
		}
		$scope.search = function(params, callback) {
			var facet = $.map(params, function(param) {
				return $.map(param, function(value, key) { return key + ':' + value }).join(',');
			});
			var t0 = new Date().getTime();
			var requests = [ search($scope.constraints, facet) ];
			if ($scope.constraintsB.length > 0) {
				requests.push(search($scope.constraintsB, facet));
			}
			$q.all(requests).then(function(responses) {
				var t1 = new Date().getTime();
				callback(responses[0].data, responses.length > 1 ? responses[1].data : null);
				tracker.timing('action', 'refresh', t1 - t0, $scope.bucketId);
			}, function(e) {
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
		$scope.getExportUrl = function() {
			var url = '/buckets/' + $scope.bucketId + '/';
			var params = {};
			if ($scope.constraints.length > 0) {
				params.q = $scope.constraints;
			}
			if (token.get()) {
				params.code = token.get();
			}
			if (!$.isEmptyObject(params)) {
				url += '?' + $.param(params, true); 
			}
			return url;
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
			taskRunner.runAll($scope, $scope.bucketId, function() {
				delay($scope.refresh);
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
			return value ? $.map(value, function(s) { return Constraint.parse(s) }) : [];
			
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
			return items != null ? items.join('|') : null;
		};
		function containsConstraint(constraint) {
			return $.grep($scope.constraints, function(c) {
				return angular.equals(c, constraint);
			}).length > 0;
		};
		function mapToString(values) {
			return values.length > 0
				? $.map(values, function(value) { return value.toString(); })
				: null;
		}
		function params() {
			var params = {};
			if ($scope.constraints.length > 0) {
				params.q = mapToString($scope.constraints);
			}
			if ($scope.constraintsB.length > 0) {
				params.r = mapToString($scope.constraintsB);
			}
			return params;
		}
		$scope.addConstraint = function(field, value, replace) {
			var constraint = new Constraint(field, value);
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
		$scope.getConstraintIcon = function(constraint) {
			var fieldName = constraint.field;
			var dot = fieldName.indexOf('.');
			if (dot != -1) {
				fieldName = fieldName.substring(0, dot);
			}
			var field = Field.find(fieldName);
			return field ? field.icon : 'icon-ban-circle';
		};
		$scope.swapAB = function() {
			var tmp = $scope.constraints;
			$scope.constraints = $scope.constraintsB;
			$scope.constraintsB = tmp;
			$location.search(params());
		};

		$scope.dirty = false;
		$scope.setDirty = function(dirty) {
			$scope.dirty = dirty;
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

	app.controller('EditBucketDialogController', ['$scope', '$http', '$route', 'delay', 'tracker', function($scope, $http, $route, delay, tracker) {

		$scope.init = function() {
			$scope.newBucket = angular.copy($scope.$parent.bucket);
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
					}).length == 0;
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
				.success(function (response, status, headers) {
					$scope.closeDialog();
					$scope.alert.show('Saved settings.', 'alert-success', headers('X-Command-ID'));
					delay($route.reload);
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
	}]);
	
	app.controller('ListWidgetController', ['$scope', function($scope) {
	
		$scope.init = function() {
			$scope.offset = 0;
			$scope.total = 0;
			$scope.items = null;
		};
		$scope.hasPrev = function() {
			return $scope.offset > 0;
		}
		$scope.hasNext = function() {
			return $scope.offset + $scope.settings.limit < $scope.total;
		}
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.settings.limit });
		}
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.settings.limit });
		}
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'list',
				offset : 0, 
				limit : $scope.settings.limit,
				order : $scope.settings.order,
				reverse : $scope.settings.reverse
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
			$scope.total = result.total;
			$scope.items = result[$scope.settings.id] || [];
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.factory('WidgetDialogControllerSupport', ['Field', function(Field) {
		return function($scope) {
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
		};
	}]);

	app.controller('WidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', function($scope, WidgetDialogControllerSupport) {
		WidgetDialogControllerSupport($scope);
	}]);
	
	app.controller('CountWidgetController', ['$scope', function($scope) {
	
		$scope.init = function() {
			$scope.offset = 0;
			$scope.more = false;
			$scope.terms = null;
		};
		$scope.hasPrev = function() {
			return $scope.offset > 0;
		}
		$scope.hasNext = function() {
			return $scope.more;
		}
		$scope.prev = function() {
			$scope.refresh({ offset : $scope.offset - $scope.settings.limit });
		}
		$scope.next = function() {
			$scope.refresh({ offset : $scope.offset + $scope.settings.limit });
		}
		$scope.setOrder = function(order) {
			$scope.refresh({ offset : 0 }, { order : order, reverse : order === $scope.settings.order && !$scope.settings.reverse });
		}
		$scope.getClasses = function(column) {
			var classes = [];
			if (column === $scope.order) {
				classes.push('caret-active');
				classes.push($scope.reverse ? 'caret-inverted' : 'caret');
			} else {
				classes.push('caret');
			}
			return classes;
		}
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'count',
				field : $scope.settings.field, 
				offset : $scope.offset, 
				limit : $scope.settings.limit,
				order : $scope.settings.order,
				reverse : $scope.settings.reverse
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
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
			$scope.addConstraint($scope.settings.field, term.label)
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('CountWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		WidgetDialogControllerSupport($scope);

		$scope.getFields = function() {
			return Field.findByType('text');
		};
	}]);

	app.controller('GanttWidgetController', ['$scope', 'timezone', function($scope, timezone) {
	
		$scope.init = function() {
			$scope.terms = null;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'gantt',
				field : $scope.settings.field, 
				timezone : timezone,
				order : $scope.settings.order,
				limit : $scope.settings.limit
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
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
			$scope.addConstraint($scope.settings.field, term.label)
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('GanttWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		WidgetDialogControllerSupport($scope);

		$scope.getFields = function() {
			return Field.findByType('text');
		};
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
				filter : $scope.settings.filter || ''
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

	app.controller('HistogramWidgetController', ['$scope', '$timeout', 'Field', function($scope, $timeout, Field) {
	
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
				filter : $scope.settings.filter || ''
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result) {
			$scope.intervals = result[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
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
					title : null,
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
		}

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('HistogramWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		WidgetDialogControllerSupport($scope);

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0
				? $scope.settings.unit === null
				: $.inArray($scope.settings.unit, units) != -1;
		};

		$scope.getField = function() {
			return Field.find($scope.settings.field);
		}
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

	app.controller('ScoreboardWidgetController', ['$scope', function($scope) {
	
		$scope.init = function() {
			$scope.terms = null;
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
				filter : $scope.settings.filter || ''
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.terms = result[$scope.settings.id] || [];
		};
		$scope.filter = function(term) {
			$scope.addConstraint($scope.settings.key_field, term.label)
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('ScoreboardWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		WidgetDialogControllerSupport($scope);

		$scope.isUnitValid = function() {
			var units = $scope.getUnits();
			return units.length === 0
				? $scope.settings.unit === null
				: $.inArray($scope.settings.unit, units) != -1;
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
		$scope.$watch('settings.value_field', function() {
			if (!$scope.isUnitValid()) {
				$scope.settings.unit = null;
			}
		});
	}]);

	app.factory('Interval', function() {

		var Interval = function(name, pattern, minTickInterval) {
			this.name = name;
			this.pattern = pattern.length;
			this.minTickInterval = minTickInterval;
		}

		Interval.VALUES = [
			new Interval('year', 'yyyy', 366 * 24 * 60 * 60 * 1000),
			new Interval('month', 'yyyy-MM', 28 * 24 * 60 * 60 * 1000), 
			new Interval('day', 'yyyy-MM-dd', 24 * 60 * 60 * 1000), 
			new Interval('hour', 'yyyy-MM-ddTHH', 60 * 60 * 1000), 
			new Interval('minute', 'yyyy-MM-ddTHH:mm', 60 * 1000),
			new Interval('second', 'yyyy-MM-ddTHH:mm:ss', 1000)
		];

		Interval.match = function(value) {
			if (value.match(/^[0-9]{4}/)) {
				if (!value.match(/Z|[+-]\d\d:\d\d/)) {
					var i, max;
					for (i = 1, max = Interval.VALUES.length; i < max; ++i) {
						if (value.length === Interval.VALUES[i - 1].pattern) {
							return Interval.VALUES[i];
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

		Interval.matchRange = function(value, expectOffset) {
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

	app.controller('TimelineWidgetController', ['$scope', '$timeout', 'Field', 'Interval', function($scope, $timeout, Field, Interval) {

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

		$scope.init = function() {
			$scope.times = null;
			$scope.timesB = null;
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
				var interval = Interval.match(prefix) || Interval.matchRange(prefix);
				if (interval) {
					$scope.interval = interval;
					$scope.range = prefix;
				}
			}
			return { 
				id : $scope.settings.id,
				type : 'timeline',
				field : $scope.settings.field,
				unit : $scope.settings.unit || '',
				interval : $scope.interval.name,
				range : $scope.range,
				filter : $scope.settings.filter || ''
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options);
				$scope.settings = $.extend({}, $scope.settings, settings); // create new settings object to trigger watch
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.times = result[$scope.settings.id] || [];
			$scope.timesB = resultB && resultB[$scope.settings.id] || [];
			$timeout($scope.draw, 1); // delay for correct width
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
								var from = '*';
								var to = '*';
								$.each($scope.times, function(i, time) {
									if (time.time >= event.xAxis[0].min) {
										from = time.label;
										return false;
									}
								});
								$.each($scope.times, function(i, time) {
									if (time.time >= event.xAxis[0].max) {
										to = time.label;
										return false;
									}
								});
								if (from != '*' || to != '*') {
									var range = '[' + from + '..' + to + ')';
									$scope.$apply(function() {
										$scope.addConstraint($scope.keyField, range, true);
									});
								}
								return false;
							}
						}
					},
					title : null,
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
						gridLineWidth : 0
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
						fillColor : 'rgba(47, 126, 216, 0.4)',
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
					credits: {
						enabled: false
					}
				};
				if ($scope.interval != Interval.VALUES[Interval.VALUES.length - 1]) {
					options.plotOptions.series.cursor = 'pointer';
					options.plotOptions.series.events = {
						click : function(event) {
							$scope.$apply(function() {
								$scope.addConstraint($scope.keyField, event.point.options.filter, true);
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
							options.series[1].data.push([ time.time, field.toNumber(time['min']), field.toNumber(time['max']) ]);
						}
					} else {
						options.series[0].data.push({ x : time.time, y : null });
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
						fillColor : 'rgba(204, 102, 0, 0.4)',
						zIndex: 0
					});
					$.each($scope.timesB, function(i, time) {
						var value = time[$scope.settings.statistic || 'count'];
						if (value !== undefined) {
							options.series[2].data.push({ x : time.time, y : field.toNumber(value), filter : time.label, tooltip : field.toText(value) });
							if ($scope.settings.statistic === 'avg') {
								options.series[3].data.push([ time.time, field.toNumber(time['min']), field.toNumber(time['max']) ]);
							}
						}
					});
				}
				field.formatAxis(options.yAxis);
				$scope.chartOptions = options;
			}
		}

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('EffectSizeWidgetController', ['$scope', '$timeout', 'Field', 'statistics', function($scope, $timeout, Field, statistics) {

		$scope.init = function() {
			$scope.stats = null;
			$scope.statsB = null;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id + '-stats',
				type : 'stats',
				field : $scope.settings.field,
				unit : $scope.settings.unit || '',
				filter : $scope.settings.filter || ''
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			if ($scope.settings.statistic === 'avg') {
				$scope.stats = result[$scope.settings.id + '-stats'];
				$scope.statsB = resultB && resultB[$scope.settings.id + '-stats'];
			}
			$timeout($scope.draw, 1); // delay for correct width
		};
		function toNumber(field, stats) {
			return {
				count : stats.count,
				avg : field.toNumber(stats.avg),
				stdev : field.toNumber(stats.stdev)
			};
		}
		$scope.draw = function() {
			if ($scope.stats && $scope.statsB) {
				var field = Field.find($scope.settings.field);
				var effect = statistics.effect(toNumber(field, $scope.stats), toNumber(field, $scope.statsB));
				$scope.rChartOptions = {
					chart : {
						type : 'line',
						inverted : true,
						height : 75,
						plotBorderWidth : 1,
						plotBackgroundColor : '#fafafa',
						marginLeft : 35,
						animation : false
					},
					title : null,
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
					series : [{
						data : [[ 0, effect.r ]],
						color : '#C0C0C0',
						animation : false,
						marker : {
							radius : 5,
							symbol : 'circle'
						},
						tooltip : {
							headerFormat : '',
							pointFormat : "<b>r:</b> {point.y}<br/>",
							valueDecimals : 3
						},
						states : {
							hover : {
								enabled : false
							}
						}
					}, {
						type : 'errorbar',
						data : [[ 0, effect.lower, effect.upper ]],
						lineWidth : 2,
						color : '#C0C0C0',
						animation : false,
						tooltip : {
							headerFormat : '',
							pointFormat : '<b>95% confidence interval:</b> [' + effect.lower.toFixed(3) + '..' + effect.upper.toFixed(3) + ']<br/>' 
						}
					}],
					legend : {
						enabled : false
					},
					credits : {
						enabled : false
					}
				};
			}
		}

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$scope.$watch('settings', function(to, from) {
			if (!angular.equals(to, from)) {
				$scope.refresh();
			}
		});
	}]);

	app.controller('TimelineWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', 'Interval', function($scope, WidgetDialogControllerSupport, Field, Interval) {

		WidgetDialogControllerSupport($scope);

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0
				? $scope.settings.unit === null
				: $.inArray($scope.settings.unit, units) != -1;
		};
		function isStatisticValid() {
			return $.grep($scope.getStatistics($scope.settings.field), function(statistic) {
				return $scope.settings.statistic === statistic;
			}).length > 0;
		};

		$scope.init = function() {
			$scope.settings = angular.copy($scope.$parent.settings);
			$scope.settings.interval = $scope.settings.interval || Interval.VALUES[1].name;
		}
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

	app.controller('PolarWidgetController', ['$scope', '$timeout', 'Field', function($scope, $timeout, Field) {

		$scope.keyField = 'timestamp';

		$scope.init = function() {
			$scope.times = null;
			$scope.timesB;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'polar',
				key_field : $scope.keyField,
				value_field : $scope.settings.value_field || null,
				unit : $scope.settings.unit || '',
				interval : $scope.settings.interval,
				filter : $scope.settings.filter || ''
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.times = result[$scope.settings.id] || [];
			$scope.timesB = resultB && resultB[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length) {
				var type = $scope.settings.statistic === 'count' || $scope.settings.statistic === 'sum' ? 'column' : 'line';
				var field = Field.find($scope.settings.value_field);
				var options = {
					chart : {
						type : 'column',
						polar: true,
						animation : false
					},
					title : null,
					xAxis : {
						categories : []
					},
					yAxis : {
						title : {
							text : null
						}
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
										$scope.addConstraint($scope.keyField + '.' + $scope.settings.interval, $scope.times[event.point.x].value, false);
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
						color: 'rgba(204, 102, 0, 0.4)'
					});
					$.each($scope.timesB, function(i, time) {
						var value = time[$scope.settings.statistic || 'count'];
						options.xAxis.categories.push(time.label);
						options.series[1].data.push(value !== undefined ? field.toNumber(value) : 0);
					});
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

		WidgetDialogControllerSupport($scope);

		$scope.intervals = [
			{ id : 'hour_of_day', label : 'hour of day' },
			{ id : 'day_of_week', label : 'day of week' },
			{ id : 'day_of_month', label : 'day of month' },
			{ id : 'month_of_year', label : 'month of year' }
		];

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0
				? $scope.settings.unit === null
				: $.inArray($scope.settings.unit, units) != -1;
		};
		function isStatisticValid() {
			return $.grep($scope.getStatistics($scope.settings.value_field), function(statistic) {
				return $scope.settings.statistic === statistic;
			}).length > 0;
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

			for (var i = 0; i < n; ++i) {
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
		 * Computes the effect size. See http://measuredme.com/2012/09/personal-analytics-101-testing-differences-in-quantified-self-data-html/
		 */
		function effect(a, b) {
			var n = a.count + b.count;
			var stdev = Math.sqrt((Math.pow(a.stdev, 2) * (a.count - 1) + Math.pow(b.stdev, 2) * (b.count - 1)) / (n - 2));
			var g = ((a.avg - b.avg) / stdev) * ((n - 3) / (n - 2.25)) * Math.sqrt((n - 2) / n);
			var r = Math.sqrt(Math.pow(g, 2) / (4 + Math.pow(g, 2)));
		  var c = confidence(r, n)
		  return {
		  	r : r,
				lower : c[0],
				upper : c[1]
		  };
		}

		function mean(a) {
			var r = { mean : 0, variance : 0, deviation : 0 };
			var t = a.length;
			for (var m, s = 0, l = t; l--; s += a[l]);
			for (m = r.mean = s / t, l = t, s = 0; l--; s += Math.pow(a[l] - m, 2));
			r.deviation = Math.sqrt(r.variance = s / t);
			return r;
		}

		/**
		 * Computes the 95% confidence interval for a correlation coefficient. 
		 * Based on http://stats.stackexchange.com/a/18904.
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
			  var ypred = [];
			  for (i = 0; i < data.length; ++i) {
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
		    return {
					data : [
						[x[min], params.slope * x[min] + params.intercept],
						[x[max], params.slope * x[max] + params.intercept]
					],
					slope : params.slope,
					intercept : params.intercept
		    };		
			},
			correlate : function(data, ranked) {
			  var x = [];
			  var y = [];
			  for (i = 0; i < data.length; ++i) {
			  	x.push(data[i][0]);
	        y.push(data[i][1]);
			  }
			  if (ranked) {
			  	x = rank(x);
			  	y = rank(y);
			  }
			  var r = pearson(x, y);
			  var c = confidence(r, x.length)
		    return {
					r : r,
					lower : c[0],
					upper : c[1]
		    };		
			},
			effect : effect
		};
	});

	app.controller('ScatterPlotWidgetController', ['$scope', '$timeout', 'Field', 'timezone', 'statistics', function($scope, $timeout, Field, timezone, statistics) {

		$scope.init = function() {
			$scope.data = null;
		};
		$scope.params = function() {
			return {
				id : $scope.settings.id,
				type : 'scatterplot',
				field_x : $scope.settings.field_x,
				unit_x : $scope.settings.unit_x || '',
				statistic_x : $scope.settings.statistic_x || 'avg',
				filter_x : $scope.settings.filter_x || '',
				field_y : $scope.settings.field_y,
				unit_y : $scope.settings.unit_y || '',
				statistic_y : $scope.settings.statistic_y || 'avg',
				filter_y : $scope.settings.filter_y || '',
				interval : $scope.settings.interval || 'day',
				timezone : timezone,
				lag : $scope.settings.lag || 0
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result, resultB) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result, resultB);
			});
		};
		$scope.update = function(event, result, resultB) {
			$scope.data = result[$scope.settings.id] || [];
			$scope.dataB = resultB && resultB[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.draw = function() {
			var xField = Field.find($scope.settings.field_x);
			var yField = Field.find($scope.settings.field_y);
			if ($scope.data && $scope.data.length) {
				var options = {
					chart : {
						type : 'scatter',
						zoomType: 'xy',
						animation : false
					},
					title : null,
					xAxis : {
						title : {
							text : $scope.settings.label_x || $scope.settings.field_x
						},
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 0,
						gridLineWidth : 0
					},
					yAxis : {
						title : {
							text : $scope.settings.label_y || $scope.settings.field_y
						},
						tickLength : 5,
						tickWidth : 1,
						lineWidth : 0,
						gridLineWidth : 0
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
					options.series.push({
						type : 'line',
						data : statistics.regression($scope.data).data,
						color : 'rgb(119, 152, 191)',
						enableMouseTracking : false,
						marker : {
							enabled : false
						}
					});
				}
				if ($scope.dataB && $scope.dataB.length > 1 && $scope.settings.regression == 'linear') {
					options.series.push({
						type : 'line',
						data : statistics.regression($scope.dataB).data,
						color : 'rgb(204, 102, 0)',
						enableMouseTracking : false,
						marker : {
							enabled : false
						}
					});
				}
				if ($scope.data.length > 3) {
					var correlation = statistics.correlate($scope.data, true);
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
						title : null,
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
						series : [{
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
						}, {
							type : 'errorbar',
							data : [[ 0, correlation.lower, correlation.upper ]],
							lineWidth : 2,
							color : 'rgb(119, 152, 191)',
							animation : false,
							tooltip : {
								headerFormat : '',
								pointFormat : '<b>95% confidence interval:</b> [' + correlation.lower.toFixed(3) + '..' + correlation.upper.toFixed(3) + ']<br/>' 
							}
						}],
						legend : {
							enabled : false
						},
						credits : {
							enabled : false
						}
					};
					if ($scope.dataB && $scope.dataB.length > 1) {
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
								pointFormat : '<b>95% confidence interval:</b> [' + correlation.lower.toFixed(3) + '..' + correlation.upper.toFixed(3) + ']<br/>' 
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

		WidgetDialogControllerSupport($scope);

		function isUnitValid(field, unit) {
			var units = $scope.getUnits(field);
			return units.length === 0
				? unit === null
				: $.inArray(unit, units) != -1;
		};

		$scope.regressionMethods = [ 'linear' ];

		$scope.getFields = function() {
			return Field.findByType('numeric');
		};
		$scope.getIntervals = function() {
			return Interval.VALUES;
		};
		$scope.getStatistics = function(field) {
			return [ 'sum', 'avg', 'min', 'max', 'count' ];
		};
		$scope.getUnits = function(field) {
			return field && Field.find(field).units || [];
		};
		$scope.valid = function() {
			return isUnitValid($scope.settings.field_x, $scope.settings.unit_x)
				&& isUnitValid($scope.settings.field_y, $scope.settings.unit_y);
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

	app.controller('MapWidgetController', ['$scope', 'googleApiKey', function($scope, googleApiKey) {

		$scope.field = 'location';
	
		$scope.init = function() {
			$scope.points = null;
			$scope.map = null;
			$scope.settings.marker_color = $scope.settings.marker_color || 'red';
			$scope.settings.factor = 'factor' in $scope.settings ? $scope.settings.factor : 0.2;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'map',
				field : 'location', 
				factor : $scope.settings.factor
			};
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$scope.search([ $.extend($scope.params(), settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.points = result[$scope.settings.id] || [];
			$scope.draw();
		};
		$scope.filterBounds = function() {
			$scope.addConstraint($scope.field, $scope.map.getBounds().toUrlValue(3), true);
		};
		$scope.draw = function() {
			if ($scope.points.length) {
				google.load('maps', '3.14', { other_params : 'libraries=places&sensor=false&key=' + googleApiKey, callback : function() {
					var options = {
						mapTypeId: google.maps.MapTypeId.TERRAIN,
						streetViewControl: false,
						mapTypeControlOptions : {
							style : google.maps.MapTypeControlStyle.DROPDOWN_MENU
						}
					};
					$scope.map = new google.maps.Map(document.getElementById($scope.settings.id + '-map'), options);

					var bounds = new google.maps.LatLngBounds();
					$.each($scope.getConstraints($scope.field), function(i, constraint) {
						var c = constraint.value.split(',');
						var sw = new google.maps.LatLng(c[0], c[1]);
						var ne = new google.maps.LatLng(c[2], c[3]);
						bounds = new google.maps.LatLngBounds(sw, ne);
					});
					var filtered = !bounds.isEmpty();
					$.each($scope.points, function(i, point) {
						var latLng = new google.maps.LatLng(point.lat, point.lon);
						var marker = new google.maps.Marker({
							position : latLng, 
							map : $scope.map,
							title : point.count > 1 ? point.count + ' events' : '1 event',
							icon: {
						    path: google.maps.SymbolPath.CIRCLE,
						    fillOpacity: 0.5,
						    fillColor: $scope.settings['marker_color'],
						    strokeOpacity: 1.0,
						    strokeColor: $scope.settings['marker_color'],
						    strokeWeight: 1.0,
						    scale: 10 + (5 * Math.log(point.count))
						  }
						});
						if (point.count === 1) {
							point.lat_min = point.lat;
							point.lat_max = point.lat;
							point.lon_min = point.lon;
							point.lon_max = point.lon;							
						}
						var sw = new google.maps.LatLng(point.lat_min - 0.001, point.lon_min - 0.001);
						var ne = new google.maps.LatLng(point.lat_max + 0.001, point.lon_max + 0.001);
						var filterBounds = new google.maps.LatLngBounds(sw, ne);
						google.maps.event.addListener(marker, 'click', function() {
							$scope.$apply(function() {
								$scope.addConstraint($scope.field, filterBounds.toUrlValue(3), true);
							});
						});
						if (point.count > 1) {
							var filterRectangle = new google.maps.Rectangle({
								bounds : filterBounds,
								strokeWeight : 1,
								fillOpacity : 0,
								clickable : false,
								visible : false,
								map : $scope.map
							});
							google.maps.event.addListener(marker, 'mouseover', function() {
								filterRectangle.setVisible(true);
							});
							google.maps.event.addListener(marker, 'mouseout', function() {
								filterRectangle.setVisible(false);
							});
						}
						if (!filtered) {
							bounds.extend(sw);
							bounds.extend(ne);
						}
					});
					$scope.map.fitBounds(bounds);
					if (filtered) {
						var world = [
							new google.maps.LatLng(-90, -180),
							new google.maps.LatLng(90, -180),
							new google.maps.LatLng(90, 0),
							new google.maps.LatLng(90, 180),
							new google.maps.LatLng(-90, 180),
							new google.maps.LatLng(-90, 0)
						];
						var area = [
							bounds.getSouthWest(),
							new google.maps.LatLng(bounds.getSouthWest().lat(), bounds.getNorthEast().lng()),
							bounds.getNorthEast(),
							new google.maps.LatLng(bounds.getNorthEast().lat(), bounds.getSouthWest().lng())
						];
						new google.maps.Polygon({
							paths : [ world, area ],
							strokeWeight: 0,
							clickable : false,
							map : $scope.map							
						});
					}
				  $scope.map.controls[google.maps.ControlPosition.TOP_RIGHT].push($scope.createFilterControl());
				}});
			} else {
				$('#' + $scope.settings.id + 'map').html('<i class="none">None</i>');
			}
		};
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
			google.maps.event.addDomListener(control, 'click', function() {
				$scope.$apply(function() {
					$scope.filterBounds();
				});
			});
			return parent;
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$('#' + $scope.settings.id + '-tab').on('show', $scope.draw);
	}]);

	app.controller('MapWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', function($scope, WidgetDialogControllerSupport) {

		WidgetDialogControllerSupport($scope);

		$scope.getColors = function() {
			return [ 'white', 'black', 'red', 'green', 'blue', 'yellow' ];
		};
	}]);

	app.factory('Event', function() {

		var Event = function(data) {
			$.extend(true, this, data);
		}

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
					.success(function(response) {
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

	app.controller('CreateLocationFieldController', ['$scope', 'googleApiKey', function($scope, googleApiKey) {

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
			google.load('maps', '3.14', { other_params : 'libraries=places&sensor=false&key=' + googleApiKey, callback : function() {
				var center = new google.maps.LatLng(0, 0);
				var options = {
					center : center,
					zoom : 2,
					mapTypeId: google.maps.MapTypeId.TERRAIN,
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
				input.on('input', function(e) {
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
			}});
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
			var time = ($scope.time.length == 7 ? '0' : '') + $scope.time + '.000';
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
			$scope.time = moment().seconds(0).format('H:mm:ss');
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
	
	app.controller('CreateResourceFieldController', ['$scope', '$http', function($scope, $http) {

		$scope.init = function() {
			$scope.value = {};
		};
		$scope.addField = function() {
			$scope.event.add($scope.field, $scope.value);
			$scope.reset();
		};
		$scope.prefillTitle = function() {
			$http.get('/og?' + $.param({ url : $scope.value.url }))
				.success(function(response) {
					$scope.value.title = response.title;
				});
		};
		$scope.valid = function() {
			return $scope.value.url && $scope.value.title;
		};
		$scope.change = function(e) {
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
	
	
	app.controller('ImportDialogController', ['$scope', '$http', '$routeParams', 'tracker', 'delay', function($scope, $http, $routeParams, tracker, delay) {

		$scope.bucketId = $routeParams.bucketId;

		$scope.init = function() {
			$scope.message = '';
			$scope.events = [];
			tracker.event('dialog', 'import events');
			$('#select-import-file').fileupload('reset');
		};
		$scope.isEmpty = function() {
			return !$scope.events || $scope.events.length == 0;
		};
		$scope.setFiles = function(files) {
			$scope.events = [];
			$scope.$apply(function(scope) {
				var reader = new FileReader();
				reader.onload = function(e) {
					scope.$apply(function(scope) {
						try {
							scope.events = JSON.parse(e.target.result);
						} catch(error) {
							$scope.message = 'Can\'t read the file. Is the format valid?';
						}
					});
				};
				reader.readAsText(files[0]);
			});
		};
		$scope.submit = function() {
			$scope.alert.clear();
			$http.post('/buckets/' + $scope.bucketId + '/', $.isArray($scope.events) ? { 'events' : $scope.events } : $scope.events)
				.success(function(response, status, headers) {
					$scope.alert.show('Imported events.', 'alert-success', headers('X-Command-ID'));
					delay($scope.refresh);
					$scope.closeDialog();
				})
				.error(function(response) {
					$scope.message = response.message || 'Couldn\'t import the file. Try again later, or contact support.';
				});
			tracker.event('action', 'import events');
		};
	}]);

	app.factory('taskRunner', [ '$http', '$window', function($http, $window) {

		var runAll = function($scope, bucketId, success) {
			$http.get('/buckets/' + bucketId + '/tasks/').success(function(response) {
				if (response.total > 0) {
					$.each(response.tasks, function(i, task) {
						run($scope, task['@id'], success);
					});
				} else {
					success();
				}
			});
		};

		var run = function($scope, taskId, success) {
			$http.get('/tasks/' + taskId)
				.success(function(response, status, headers) {
					if (headers('X-Credentials')) {
						newCredentials($scope, headers('X-Credentials'));
					} else if (headers('Link')) {
						var match = headers('Link').match(/<(.+?)>/);
						console.assert(match, 'Invalid Link header: ' + headers('Link'));
						authorize($scope, response.type, match[1]);
					} else {
						success(response);
					}
				})
				.error(function(response, status) {
					if (status == 403) {
						$scope.alert.show('Couldn\'t refresh task. Insufficient quota?', 'alert-error');
					} else if (status < 500) {
						$scope.alert.show('Couldn\'t refresh task.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t refresh task. Try again later or contact support.', 'alert-error');
					}
				});		
		};

		var newCredentials = function($scope, type) {
			$http.post('/credentials/', { type : type })
				.success(function(response, status) {
					console.assert(status === 201, status);
					if (response.authorizationUrl) {
						authorize($scope, type, response.authorizationUrl);
					}
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.alert.show('Can\'t create credentials: ' + response.message, 'alert-error');					
					} else {
						$scope.alert.show('Couldn\'t create credentials. Please try again later or contact support.', 'alert-error');					
					}
				});
		};

		var authorize = function($scope, type, url) {
			$scope.alert.show('<b>' + type + '</b> requires authorization', '', '', function() {
				$window.open(url);
			});
		}
		
		return {
			runAll : runAll,
			run : run
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
				.success(function(response, status, headers) {
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
	}]);

	app.controller('CreateTaskDialogController', ['$scope', '$http', 'delay', 'tracker', function($scope, $http, delay, tracker) {
	
		$scope.types = [ 
      { 'id' : 'foursquare', 'description' : 'Creates an event for each check-in.' },
			{ 'id' : 'fitbit-sleep', 'description' : 'Creates an event for each period of sleep.' },
			{ 'id' : 'fitbit-steps', 'description' : 'Creates an event for the number of steps each day (incl distance and elevation, if available).' },
			{ 'id' : 'bodymedia-burn', 'description' : 'Creates an event for the number of calories burned each hour.' },
			{ 'id' : 'bodymedia-sleep', 'description' : 'Creates an event for each period of sleep.' },
			{ 'id' : 'bodymedia-steps', 'description' : 'Creates an event for the number of steps each hour.' },
			{ 'id' : 'netatmo', 'description' : 'Creates an event for each weather station measurement.' },
			{ 'id' : 'runkeeper-activities', 'description' : 'Creates an event for each logged activity.' },
			{ 'id' : 'withings', 'description' : 'Creates an event for each body weight measurement.' },
			{ 'id' : 'demo', 'description' : 'Creates an event with a custom tag.' }
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
				.success(function(response, status, headers) {
					console.assert(status === 201, status);
					$scope.closeDialog();
					delay($scope.$parent.run);
				})
				.error(function(response) {
					$scope.message = 'Couldn\'t create task. Try again later or contact support.';
				});
			tracker.event('action', 'create task');
		};
	}]);

	app.controller('FitbitSleepSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'sleep',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitStepsSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'steps',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('BodyMediaBurnSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'burn',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('BodyMediaSleepSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'sleep',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('BodyMediaStepsSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'steps',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FoursquareSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('NetatmoSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf())
			};
		};	

		$scope.init();
	}]);

	app.controller('RunkeeperSettingsController', ['$scope', '$http', 'Field', 'googleApiKey', function($scope, $http, Field, googleApiKey) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					unit : 'km',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf()),
					timezone : 'UTC'
			};
			$http.get('/tz').success(function(response) {
				$scope.timezones = response;
				if (navigator.geolocation) {
					navigator.geolocation.getCurrentPosition(function(position) {
						$http.get('/tz?' + $.param({ 'lat' : position.coords.latitude, 'lon' : position.coords.longitude }))
							.success(function(response) {
								$scope.settings.timezone = response.timeZoneId;
							});
					});
				}
			});
		};
		$scope.getUnits = function() {
			return Field.find('distance').units;
		};

		$scope.init();
	}]);

	app.controller('WithingsSettingsController', ['$scope', '$http', 'Field', 'googleApiKey', function($scope, $http, Field, googleApiKey) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'body',
					unit : 'kg',
					marker : new Date(moment().utc().subtract('months', 3).startOf('month').valueOf()),
					timezone : 'UTC'
			};
			$http.get('/tz').success(function(response) {
				$scope.timezones = response;
				if (navigator.geolocation) {
					navigator.geolocation.getCurrentPosition(function(position) {
						$http.get('/tz?' + $.param({ 'lat' : position.coords.latitude, 'lon' : position.coords.longitude }))
							.success(function(response) {
								$scope.settings.timezone = response.timeZoneId;
							});
					});
				}
			});
		};
		$scope.getUnits = function() {
			return Field.find('weight').units;
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

	app.controller('CredentialsController', ['$scope', '$http', '$routeParams', '$location', '$window', function($scope, $http, $routeParams, $location, $window) {
		
		$scope.credentialsId = $routeParams.credentialsId;

		$http.post('/credentials/' + $scope.credentialsId, { 'credentials' : $location.search() })
			.success(function(response) {
				$scope.alert.show('Updated credentials.', 'alert-success');
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

	app.controller('PricingController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {

		$scope.isPlan = function(quota) {
			if ($scope.quota) {
				return $scope.quota.limit == quota; 
			}
		};
		$scope.selectPlan = function(quota) {
			$scope.openDialog('plan-dialog');
			tracker.event('action', 'select plan', quota);
		};

		$scope.$watch('user', function(user) {
			if (user) {
				$http.get('/quota')
					.success(function(response) {
						$scope.quota = response;
					})
					.error(function(response, status) {
						$scope.quota = null;
					});
			}
		});
	}]);

	app.factory('Field', ['User', 'moment', function(User, moment) {

		var fields = [];
		var fieldsByName = {};

		var Field = function(name, icon, type, units, readOnly, toText, toHtml, toNumber, formatAxis) {
			this.name = name;
			this.icon = icon;
			this.type = type;
			this.units = units;
			this.readOnly = readOnly;
			this.toText = toText;
			this.toHtml = toHtml;
			this.toNumber = toNumber;
			this.formatAxis = formatAxis;
		}

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
		}
		
		Field.findAll = function() {
			return fields;
		}

		Field.findEditable = function() {
			return $.grep(fields, function(field) {
				return !field.readOnly;
			});
		}

		Field.findByType = function(type) {
			return $.grep(fields, function(field) {
				return field.type === type;
			});
		}

		function encode(value) {
			return $('<div />').text(value).html();
		};

		function register(fieldOptions) {
			console.assert(fieldOptions.name, 'missing <name>');
			var field = new Field(
				fieldOptions.name, 
				fieldOptions.icon || '', 
				fieldOptions.type || 'numeric',
				fieldOptions.units || [], 
				fieldOptions.readOnly == true, 
				fieldOptions.toText || function(value) { return value; }, 
				fieldOptions.toHtml || function(value) { return value; },
				fieldOptions.toNumber || toNumber,
				fieldOptions.formatAxis || function(options) { }
			);
			fields.push(field); 
			fieldsByName[field.name] = field; 
		};

		register({
			name : 'tag',
			icon : 'icon-tag',
			type : 'text',
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="' + this.icon + '" title="Tag"></i> ' + encode(value) +
				'</span>';
			}
		});

		register({
			name : 'resource',
			icon : 'icon-bookmark',
			type : 'object',
			toHtml : function(value) {
				return '<span>' +
			  	'<i class="' + this.icon + '" title="Resource"></i>&nbsp;' +
			  	'<a href="' +  encode(value.url) + '" target="_blank" rel="nofollow">' +  encode(value.title) + '</a>' +
			  '</span>';
			}
		});

		register({
			name : 'distance',
			icon : 'icon-resize-horizontal',
			type : 'numeric',
			units : [ 'mi', 'ft', 'in', 'km', 'm', 'cm', 'mm' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Distance"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'height',
			icon : 'icon-resize-vertical',
			type : 'numeric',
			units : [ 'mi', 'ft', 'in', 'km', 'm', 'cm', 'mm' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Height"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'weight',
			icon : 'icon-leaf',
			type : 'numeric',
			units : [ 'lb', 'oz', 'kg', 'g', 'mg', 'st' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Weight"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'percentage',
			icon : 'icon-th',
			type : 'numeric',
			toText : function(value) {
				return value + '%';
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Percentage"></i> <abbr title="' + value + '%">' + Math.round(value) + '%</abbr>' +
			  '</span>';
			}
		});

		register({
			name : 'volume',
			icon : 'icon-tint',
			type : 'numeric',
			units : [ 'L', 'dL', 'cL', 'mL', 'gal', 'qt', 'pt', 'cups', 'fl_oz' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Volume"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'concentration',
			icon : 'icon-tint',
			type : 'numeric',
			units : [ 'g/L', 'mg/L', 'ug/L', 'ng/L', 'g/dL', 'mg/dL', 'ug/dL', 'ng/dL', 'g/mL', 'mg/mL', 'ug/mL', 'ng/mL' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Volume"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'humidity',
			icon : 'icon-tint',
			type : 'numeric',
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Humidity"></i> ' + value + '%' +
			  '</span>';
			}
		});

		register({
			name : 'pressure',
			icon : 'icon-fullscreen',
			type : 'numeric',
			units : [ 'Pa', 'hPa', 'mmHg', 'inHg', 'psi' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Pressure"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'sound',
			icon : 'icon-volume-up',
			type : 'numeric',
			units : [ 'dB' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Sound Level"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'location',
			icon : 'icon-map-marker',
			type : 'object',
			toText : function(value) {
				return typeof value === 'object' ? encode(Math.round(value.lat * 1000) / 1000 + ', ' + Math.round(value.lon * 1000) / 1000) : '';
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="' + this.icon + '" title="Location"></i> ' +
					'<a href="http://maps.google.com/maps?q=' + encode(value.lat + ',' + value.lon) + '&t=p&z=5" target="_blank">' + this.toText(value) + '</a>' +
				'</span>';
			}
		});

		register({
			name : 'timestamp',
			icon : 'icon-calendar',
			type : 'object',
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Timestamp"></i> ' +
					'<abbr title="' + value + '">' + moment(value).zone(value).fromNowOrNow(false) + '</abbr>' +
			  '</span>';
			}
		});

		register({
			name : 'velocity',
			icon : 'icon-road',
			type : 'numeric',
			units : [ 'm/s', 'mph', 'kmh', 'kn', 'Mach' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Velocity"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'duration',
			icon : 'icon-time',
			type : 'numeric',
			toText : function(value) {
				return value ? moment.duration(value).countdown() : 0;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Duration"></i> ' + this.toText(value) +
			  '</span>';
			},
			toNumber : function(value) {
				var n = toNumber(value);
				if (isNaN(n)) {
					var valid = true;
					$.each(value.split(' '), function(i, token) {
						var m = /^(\d+)(d|h|min|s)?$/.exec(token);
						if (m) {
							var ms = Number(m[1]);
							switch (m[2]) {
								case 'd':
									ms *= 24;
								case 'h':
									ms *= 60;
								case 'min':
									ms *= 60;
								case 's':
									ms *= 1000;
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
					formatter : function() {
						return this.value >= 0 ? moment.duration(this.value).countdown(2) : ''; 
					}
				};
			}
		});

		register({
			name : 'frequency',
			icon : 'icon-heart',
			type : 'numeric',
			units : [ 'bpm', 'Hz' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Frequency"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'bits',
			icon : 'icon-hdd',
			type : 'numeric',
			units : [ 'bit', 'B', 'KB', 'MB', 'GB', 'TB', 'PB', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Bits"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'count',
			icon : 'icon-th',
			type : 'numeric',
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Count"></i> ' + value +
			  '</span>';
			}
		});

		register({
			name : 'energy',
			icon : 'icon-fire',
			type : 'numeric',
			units : [ 'J', 'kJ', 'cal', 'kcal' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Energy"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'temperature',
			icon : 'icon-fire',
			type : 'numeric',
			units : [ 'C', 'F', 'K' ],
			toText : function(value) {
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : value;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Temperature"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'rating',
			icon : 'icon-star',
			type : 'numeric',
			toText : function(value) {
				return value + '%';
			},
			toHtml : function(value) {
				var stars = Math.round((value || 0) / 20);
				var html = '<span class="nowrap" title="' + this.toText(value) + '">';
				for (var i = 0; i < 5; ++i) {
					html += '<i class="' + (stars > i ? 'icon-star' : 'icon-star-empty') + '"></i>';
				}
				html += '</span>';
				return html;
			}
		});

		register({
			name : 'note',
			icon : 'icon-comment',
			type : 'object',
			toHtml : function(value) {
				return '<span>' +
			  	'<i class="' + this.icon + '" title="Note"></i>&nbsp;' + encode(value) +
			  '</span>';
			}
		});

		register({
			name : 'author',
			icon : 'icon-user',
			type : 'text',
			readOnly : true,
			toText : function(value) {
				return User.find(value).getName();
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="User"></i> ' + this.toText(value) +
			  '</span>';
			}
		});

		register({
			name : 'source',
			icon : 'icon-share',
			type : 'object',
			readOnly : true,
			toText : function(value) {
				return value.title;
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
					'<i class="' + this.icon + '" title="Source"></i> <a href="' +  encode(value.url) + '" rel="nofollow">' +  encode(value.title) + '</a>' +
				 '</span>';
			}
		});

		return Field;
	}]);

	app.filter('fields', ['Field', function(Field) {
		return function(event) {
			var html = '';
			var count = 0;
			$.each(Field.findAll(), function(i, field) {
				var value = event[field.name];
				if (angular.isDefined(value)) {
					$.each($.isArray(value) ? value : [ value ], function(i, value) {
						if (count > 0) {
							html += ' &nbsp; ';
						}
						html += field.toHtml(value);
						++count;
					});
				}
			});
			return html;
		}
	}]);

	app.filter('field', ['Field', function(Field) {
		return function(value, fieldName) {
			var field = Field.find(fieldName);
			console.assert(field, 'Don\'t know how to format field: ' + fieldName)
			return field.toHtml(value);
		}
	}]);

	app.filter('age', [ 'moment', function(moment) {
		return function(date) {
			return date ? moment(date).zone(date).fromNowOrNow(true) : '';
		}
	}]);

	app.filter('duration', ['moment', function(moment) {
		return function(millis) {
			return moment.duration(millis).countdown(1);
		}
	}]);

	app.filter('stars', ['Field', function(Field) {
		var field = Field.find('rating');
		return function(rating) {
			return field.toHtml(rating);
		}
	}]);

	app.filter('username', ['User', function(User) {
		return function(identity) {
			return identity ? User.find(identity).getName() : '';
		}
	}]);

	app.config(['$httpProvider', function($httpProvider) {
		var interceptor = ['$rootScope', '$q', function(scope, $q) {
			function success(response) {
				return response;
			}
			function error(response) {
				if (response.status === 401) {
					scope.$broadcast('event:unauthorized');
				}
				return $q.reject(response);
			}
			return function(promise) {
				return promise.then(success, error);
			}
		}];
		$httpProvider.responseInterceptors.push(interceptor);
	}]);

	app.directive('uiQuota', ['$interpolate', function($interpolate) {
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
							var limit = scope.$eval(attrs.uiLimit);
							var used = quota.limit - quota.remaining;
							var percent = Math.min(Math.ceil(used / limit * 100), 100);
							element.html(template({
								'title' : 'Used: ' + used + '/' + limit,
								'class' : quota.limit == limit ? 'bar-success' : 'bar-info',
								'percent' :  percent
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
			link : function(scope, element, attrs) {
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
			link : function(scope, element, attrs, model) {
				var id = attrs.id || scope.$eval(attrs.uiModal);
				console.assert(id, '@id is required');
				element.addClass('modal hide');
				element.on('hidden', function() {
					scope.closeDialog();
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

	app.directive('uiDatepicker', function() {
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
				if (controller != null) {
					controller.$formatters.unshift(function(value) {
						return moment.utc(value).format('YYYY-MM-DD');
					});
					controller.$render = function() {
						element.datepicker().data().datepicker.date = controller.$viewValue;
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
	});

	app.directive('uiTimepicker', function() {
		return {
			require : '?ngModel',
			restrict : 'A',
			link : function($scope, element, attrs, controller) {
				element.timepicker({ 
					template : false,
					minuteStep : 1,
					secondStep : 5,
					defaultTime : false,
					showMeridian : false, 
					showSeconds : true
				});
			}
		};
	});

	app.directive('uiDefer', ['$timeout', function($timeout) {
		return {
			require : 'ngModel',
			link : function($scope, $element, $attrs, modelCtrl) {
				var $setViewValue = modelCtrl.$setViewValue;
				var bufferedValue;
				modelCtrl.$setViewValue = function(value) {
					bufferedValue = value;
				}
				$element.bind('change', function() {
					$timeout(function() {
						$setViewValue.call(modelCtrl, bufferedValue);
					});
				});
			}
		}
	}]);

	app.directive('uiChartOptions', function() {
		return {
			restrict : 'A',
			scope : true,
			link : function(scope, element, attrs) {
				var defaultOptions = {
					chart : {
						renderTo : element[0]
					}
				};
				scope.$watch(attrs.uiChartOptions, function(newOptions, oldOptions) {
					if (!angular.equals(newOptions, oldOptions)) {
						if (oldOptions) {
							scope.chart.destroy();
						}
						if (newOptions) {
							scope.chart = new Highcharts.Chart($.extend(true, {}, newOptions, defaultOptions));
							$('#' + attrs.uiId + '-tab').on('shown', function(e) { 
								var parent = $(scope.chart.container).parent();
								scope.chart.setSize(parent.width(), parent.height());
								scope.chart.hasUserSize = undefined;
							});
						}
					}
				}, true);
			}
		};
	});

	app.directive('uiBucketLabel', ['Bucket', function(Bucket) {
		return {
			restrict : 'A',
			link : function(scope, element, attrs) {
				var id = scope.$eval(attrs.uiBucketLabel);
				element.html(id);
				var bucket = Bucket.getLabel(id, function(label) {
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

}());
