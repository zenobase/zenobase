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

	var app = angular.module('ZenoModule', ['ngSanitize']);

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
			if (!alwaysRelative && diff >= 129600000) { // 36 hours or more
				return this.format('MMM D, YYYY');
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
			return args.join(' ');			
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
			.when('/tasks/:taskId', { templateUrl : cacheBuster.rewrite('/partials/task.html') })
			.when('/users/:userId', { templateUrl : cacheBuster.rewrite('/partials/user.html') })
			.when('/users/:userId/reset', { templateUrl : cacheBuster.rewrite('/partials/reset.html') })
			.when('/users/:userId/verify', { templateUrl : cacheBuster.rewrite('/partials/verification.html') })
			.when('/oauth/authorize', { templateUrl : cacheBuster.rewrite('/partials/oauth.html') })
			.when('/legal/:section', { title : 'Legal', templateUrl : cacheBuster.rewrite('/partials/legal.html'), controller : 'DocumentController' })
			.when('/api/:section', { title : 'API', templateUrl : cacheBuster.rewrite('/partials/api.html'), controller : 'DocumentController' })
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
			$rootScope.page.setTitle(current.$$route.title);
		});
	}]);

	app.controller('ApplicationController', ['$scope', '$route', '$http', '$location', 'Alert', 'User', 'token', 'tracker', 'delay', function($scope, $route, $http, $location, Alert, User, token, tracker, delay) {

		$scope.alert = new Alert();

		$scope.whoami = function() {
			$http.get('/who').success(function(response) {
				$scope.user = response ? new User(response) : null;
				if ($scope.user) {
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
		$scope.openDialog = function(dialog) {
			$scope.dialog = dialog;
			$scope.$broadcast('dialog:' + dialog);
		};
		$scope.closeDialog = function() {
			$scope.dialog = null;
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
		
		Alert.prototype.show = function(message, level, undo) {
			this.message = message;
			this.level = level;
			this.undo = undo;
		};
		
		Alert.prototype.clear = function() {
			this.message = '';
			this.level = 'hide';
			this.undo = '';
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

		User.find = function(identity) {
			var user = cache.get(identity);
			if (!user) {
				$.ajax('/users/?identity=' + identity, { async : false, success : function(response) {
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
	
		$scope.userId = $routeParams.userId;
		$scope.userInfo = null;
	
		if ($scope.userId !== 'guest') {
			$http.get('/users/' + $scope.userId)
				.success(function(response) {
					$scope.userInfo = new User(response);
					$scope.page.setTitle($scope.userInfo.getName());
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.message = 'Can\'t retrieve this user.';
					} else {
						$scope.message = 'Couldn\'t retrieve this user. Try again later or contact support.';
					}
				});
		} else if ($scope.user && $scope.user.getName() === 'guest') {
			$scope.userInfo = $scope.user;
		}
	
		$scope.editable = function() {
			return $scope.user && $scope.userInfo && $scope.user.name && $scope.userInfo.name === $scope.user.name;
		};
		$scope.addable = function() {
			return $scope.user && $scope.userInfo && $scope.userInfo.getName() === $scope.user.getName();
		};
		$scope.close = function() {
			if (confirm('Close your account and delete all associated data?')) {
				tracker.event('action', 'close account');
				$http({ method : 'DELETE', url : '/users/' + $routeParams.userId })
					.success(function() {
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
	
	app.controller('UserFormController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {
	
		$scope.editing = false;

		$scope.data = function() {
			var data = {};
			if ($scope.email && $scope.email !== $scope.userInfo.email || !$scope.userInfo.verified) {
				data.email = $scope.email;
			}
			return data;
		};
		$scope.save = function() {
			$scope.alert.clear();
			var data = $scope.data();
			if (!$.isEmptyObject(data)) { 
				$http.post('/users/' + $scope.userInfo.name, data)
					.success(function(response, status, headers) {
						$scope.alert.show('Updated user info.', 'alert-success', headers('X-Command-ID'));
						$scope.editing = false;
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
		$scope.cancel = function() {
			$scope.editing = false;
		};
		$scope.$on('edit:user', function() {
			$scope.message = '';
			$scope.email = $scope.userInfo.email;
			$scope.editing = true;
			tracker.event('dialog', 'edit user');
		});
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
		$http.post('/users/' + $routeParams.userId, { 'key' : $location.search()['key'], 'verified' : true })
			.success(function() {
				$scope.alert.show('Your email address has been verified.', 'alert-success');
				$scope.whoami();
				$location.url('/users/' + $routeParams.userId);
			})
			.error(function() {
				$scope.alert.show('Your email address could not be verified.', 'alert-error');
				$location.url('/users/' + $routeParams.userId);
			});
	}]);
	
	app.controller('PasswordResetController', ['$scope', '$http', '$location', '$routeParams', 'token', function($scope, $http, $location, $routeParams, token) {

		var userId = $routeParams.userId;
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
			$http.post('/users/' + userId, { 'key' : key, 'expires' : expires, 'password' : $scope.password })
				.success(function(response) {
					console.assert(response.access_token, 'missing access_token in password reset response');
					token.set(response.access_token);
					$scope.alert.show('Your password has been changed.', 'alert-success');
					$location.url('/users/' + userId);
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
				$http.get('/buckets/?' + $.param({ 'q' : 'roles.principal:' + $scope.user['@id'], 'offset' : 0, 'limit' : 25 }))
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

	app.controller('BucketListController', ['$scope', '$http', 'tracker', function($scope, $http, tracker) {
	
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
				q : 'roles.principal:' + $scope.userInfo['@id'],
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/buckets/?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.buckets = response.buckets;
				});
		};
		$scope.remove = function(bucketId) {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/buckets/' + bucketId })
				.success(function(response, status, headers) {
					$scope.alert.show('Deleted a bucket.', 'alert-success', headers('X-Command-ID'));
					$scope.refresh({});
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

		$scope.$watch('userInfo', function(user) {
			if (user) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);

	app.controller('TaskListController', ['$scope', '$http', 'tracker', 'delay', 'tasks', function($scope, $http, tracker, delay, tasks) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.tasks = null;

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
				q : 'principal:' + $scope.userInfo['@id'],
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/tasks/?' + $.param($.extend($scope.params(), params)))
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
		$scope.refreshTask = function(taskId) {
			tasks.refresh($scope, taskId, $scope.refresh);
		};
		$scope.remove = function(taskId) {
			$scope.alert.clear();
			$http({ method : 'DELETE', url : '/tasks/' + taskId })
				.success(function(response, status, headers) {
					$scope.alert.show('Deleted a task.', 'alert-success', headers('X-Command-ID'));
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

		$scope.$watch('userInfo', function(user) {
			if (user) {
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
				q : 'principal:' + $scope.userInfo['@id'],
				client_only : true,
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/authorizations/?' + $.param($.extend($scope.params(), params)))
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

		$scope.$watch('userInfo', function(user) {
			if (user) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);

	app.controller('HomeController', ['$scope', '$http', '$location', 'token', 'tracker', function($scope, $http, $location, token, tracker) {

		var createBucket = function() {
			$http.post('/buckets/', { label : 'My Data' })
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$location.url(location);
					$scope.openDialog('getting-started-dialog');					
				})
				.error(function(response, status) {
					if (status < 500) {
						$scope.alert.show('Can\'t create a new bucket.', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t create a new bucket. Try again later or contact support.', 'alert-error');
					}
				});			
		};

		$scope.start = function() {
			$scope.alert.clear();
			$http({ method: 'POST', url: '/oauth/token', data: 'grant_type=client_credentials',
				headers: { 'Content-Type' : 'application/x-www-form-urlencoded' }
			})
				.success(function(response) {
					console.assert(response.access_token, 'missing access_token in getting started response');
					token.set(response.access_token);
					$scope.whoami();
					createBucket();
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

	app.controller('CreateBucketDialogController', ['$scope', '$http', '$location', 'tracker', function($scope, $http, $location, tracker) {

		$scope.init = function() {
			$scope.label = 'My Data';
			$scope.message = '';
			$scope.virtual = false;
			$scope.buckets = [];
			$scope.aliases = [];
			$scope.selected = null;
			$http.get('/buckets/?' + $.param({ q : 'roles.principal:' + $scope.userInfo['@id'], offset : 0, limit : 100 })).success(function(response) {
				$scope.buckets = response.buckets;
			});
			tracker.event('dialog', 'create bucket');
		};
		$scope.valid = function() {
			return !$scope.virtual || $scope.aliases.length > 0;
		};
		$scope.create = function() {
			$scope.alert.clear();
			$http.post('/buckets/', { label : $scope.label, aliases : $scope.virtual ? $scope.aliases : [] })
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.closeDialog();
					$location.url(location);
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create a new bucket with this label.';					
					} else {
						$scope.message = 'Couldn\'t create a new bucket. Please try agan later or contact support.';					
					}
				});
			tracker.event('action', 'create bucket');
		};
		$scope.listBuckets = function() {
			if ($scope.buckets) {
				return $.grep($scope.buckets, function(bucket) {
					return (!bucket.aliases || bucket.aliases.length == 0) && $.grep($scope.aliases, function(alias) {
						return alias['@id'] == bucket['@id'];
					}).length == 0;
				});
			}
		};
		$scope.addBucket = function() {
			$scope.aliases.push($scope.selected);
			$scope.selected = null;
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
      	settings : { field : 'timestamp', statistic : 'count' }
      },
      {
      	type : 'list',
      	label : 'List', 
      	description : 'Lists events.',
      	settings : { limit : 10, order : 'timestamp', reverse : false },
      	singleton : true
      },
      {
      	type : 'count',
      	label : 'Count', 
      	description : 'Counts events by tag or author.',
      	settings : { field : 'tag', order : 'count', reverse : false, limit : 5 }
      },
      {
      	type : 'map',
      	label : 'Map', 
      	description : 'Shows event locations on a map.',
      	settings : { },
      	singleton : true
      },
      {
      	type : 'ratings',
      	label : 'Ratings',
    		description : 'Counts events by their rating.',
      	settings : { }
      },
      {
      	type : 'histogram',
      	label : 'Histogram', 
      	description : 'Shows the distribution of values in a field.',
      	settings : { field : 'distance', interval : 10, unit : 'mi' }
      },
      {
      	type : 'scoreboard',
      	label : 'Scoreboard', 
      	description : 'Statistics for the values in a field',
      	settings : { key_field : 'author', value_field : 'distance', unit : 'mi', order : 'total', limit : 10 }
      },                    
	  	{
      	type : 'gantt',
      	label : 'Frequency', 
      	description : 'Shows how long ago and how often certain events occur.',
      	settings : { field : 'tag', order : 'max', limit : 10 }
      },
	  	{
      	type : 'polar',
      	label : 'Polar Chart', 
      	description : 'Plots values by month of year, day of week, or hour of day.',
      	settings : { interval : 'day_of_week', value_field : 'timestamp' }
      },
	  	{
      	type : 'scatterplot',
      	label : 'Scatter Plot', 
      	description : 'Correlates the values in two different fields.',
      	settings : { field_x : 'count', field_y : 'count' }
      }
	  ];
		$scope.init = function() {
			$scope.template = null;
		};
		$scope.add = function() {
			var settings = {
				'id' : random.id(),
				'type' : $scope.template.type,
				'label' : $scope.template.label,
				'placement' : $scope.placement
			};
			$.extend(true, settings, $scope.template.settings);
			$scope.addWidget(settings);
			$scope.chooseWidget(null);
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

	app.controller('DashboardController', ['$scope', '$http', '$route', '$routeParams', '$location', 'Bucket', 'Field', 'Constraint', 'tracker', 'delay', 'token', function($scope, $http, $route, $routeParams, $location, Bucket, Field, Constraint, tracker, delay, token) {

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
		};
		$scope.placement = null;
		$scope.canImport = function() {
			return typeof FileReader != 'undefined' && $scope.editable;
		};
		$scope.chooseWidget = function(placement) {
			$scope.placement = placement;
		};
		$scope.addWidget = function(settings) {
			$scope.bucket.widgets.push(settings);
		};
		$scope.getTemplate = function(type) {
			return cacheBuster.rewrite('/dashboard/' + type + '.html');
		};
		$scope.register = function(widget) {
			$scope.widgets.push(widget);
			if ($scope.widgets.length === $scope.bucket.widgets.length) {
				$scope.refresh();
			}
		};
	
		$scope.search = function(params, callback) {
			var q = $scope.constraints;
			var facet = $.map(params, function(param) {
				return $.map(param, function(value, key) { return key + ':' + value }).join(',');
			});
			var t0 = new Date().getTime();
			$http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'facet' : facet }, true))
				.success(function(response) { 
					var t1 = new Date().getTime();
					callback(response);
					tracker.timing('action', 'refresh', t1 - t0, $scope.bucketId);
				})
				.error(function(response) { callback({ total : -1 }) });
		};
		$scope.refresh = function() {
			$scope.updateConstraints();
			var params = $.map($scope.widgets, function(widget) { return widget.params(); });
			$scope.$broadcast('refresh');
			$scope.search(params, function(response) {
				$scope.total = response.total;
				$scope.$broadcast('result', response);
			});
		};
		$scope.params = function() {
			return null;
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
		$scope.editEvent = function(event) {
			$scope.selectedEvent = event;
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
	
		$scope.$on('$routeUpdate', function() {
			$scope.refresh();
		});
		$scope.updateConstraints = function() {
			var q = $location.search()['q'];
			$scope.constraints = q ? $.map(q.split('__'), function(s) { return Constraint.parse(s) }) : [ ];
		};
		$scope.getConstraints = function(field) {
			return $.grep($scope.constraints, function(constraint) {
				return constraint.field === field;
			});
		};
		function containsConstraint(constraint) {
			return $.grep($scope.constraints, function(c) {
				return angular.equals(c, constraint);
			}).length > 0;
		};
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
			$location.search('q', $scope.constraints.join('__'));
		};
		$scope.removeConstraint = function(constraint) {
			$scope.constraints = $.grep($scope.constraints, function(c) {
				return !angular.equals(c, constraint);
			});
			$location.search('q', $scope.constraints.length ? $scope.constraints.join('__') : null);
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
	
		$scope.dirty = false;
		$scope.setDirty = function(dirty) {
			$scope.dirty = dirty;
		};
	}]);
	
	app.controller('EditWidgetsController', ['$scope', '$http', '$route', 'tracker', function($scope, $http, $route, tracker) {
		$scope.save = function(settings) {
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

	app.controller('EditBucketDialogController', ['$scope', '$http', '$route', 'delay', 'tracker', function($scope, $http, $route, delay, tracker) {

		$scope.init = function() {
			$scope.newBucket = angular.copy($scope.$parent.bucket);
			tracker.event('dialog', 'edit bucket');
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
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.intervals = result[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.draw = function() {
			if ($scope.intervals && $scope.intervals.length) {
				var field = Field.find($scope.settings.field);
				var options = {
					chart : {
						type : 'bar',
						zoomType : 'x',
						resetZoomButton : {
							theme : {
								display : 'none'
							}
						},
						height : Math.max($scope.intervals.length * 20, 150)
					},
					title : null,
					xAxis : {
						categories : [],
						tickLength : 0,
						events : {
							setExtremes : function(event) {
								var min = (event.min !== undefined) ? Math.ceil(event.min) : 0;
								var max = (event.max !== undefined) ? Math.floor(event.max) : $scope.intervals.length - 1;
								var from = field.toText($scope.intervals[max].from); 
								var to = field.toText($scope.intervals[min].to);
								if (from != '*' || to != '*') {
									var range = '[' + from + '..' + to + ')';
									$scope.$apply(function() {
										$scope.addConstraint($scope.settings.field, range, true);
									});
								}
								return false;
							}
						}
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
						data : []
					}],
					tooltip : {
						shared : true,
						hideDelay : 0,
						crosshairs : false,
						headerFormat : '<b>{point.key}</b>: ',
						pointFormat : '{point.y}'
					},
					plotOptions : {
						series : {
							pointWidth : 10,
							borderRadius : 5,
							color : '#aaa',
							borderWidth : 2,
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
		$('#' + $scope.settings.id + '-tab').on('shown', $scope.draw);
	}]);

	app.controller('HistogramWidgetDialogController', ['$scope', 'WidgetDialogControllerSupport', 'Field', function($scope, WidgetDialogControllerSupport, Field) {

		WidgetDialogControllerSupport($scope);

		function isUnitValid() {
			var units = $scope.getUnits();
			return units.length === 0
				? $scope.settings.unit === null
				: $.inArray($scope.settings.unit, units) != -1;
		};

		$scope.getFields = function() {
			return Field.findByType('numeric');
		};
		$scope.getUnits = function() {
			var f = Field.find($scope.settings.field);
			return f ? f.units : [];
		};
		$scope.valid = function() {
			return $scope.settings.interval > 0.0;
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

		$scope.init = function() {
			$scope.times = null;
		};
		$scope.params = function() {
			$scope.interval = Interval.valueOf($scope.settings.interval) || Interval.VALUES[1];
			$scope.range = '';
			$.each($scope.getConstraints($scope.keyField), function(i, constraint) {
				var interval = Interval.match(constraint.value) || Interval.matchRange(constraint.value);
				if (interval) {
					$scope.interval = interval;
					$scope.range = constraint.value;
				}
			});
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
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.times = result[$scope.settings.id] || [];
			$timeout($scope.draw, 1); // delay for correct width
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length) {
				var type = $scope.settings.statistic === 'count' || $scope.settings.statistic === 'sum' ? 'column' : 'line';
				var field = Field.find($scope.settings.field);
				var options = {
					chart : {
						zoomType : 'x',
						resetZoomButton : {
							theme : {
								display : 'none'
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
						gridLineWidth : 0,
						events : {
							setExtremes : function(event) {
								var from = '*';
								var to = '*';
								$.each($scope.times, function(i, time) {
									if (time.time >= event.min) {
										from = time.label;
										return false;
									}
								});
								$.each($scope.times, function(i, time) {
									if (time.time >= event.max) {
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
						shared : true,
						hideDelay : 0
					},
					series : [{
						name : $scope.settings.statistic || 'count',
						type : type,
						data : [],
						zIndex: 1
					}, {
						name : 'range',
						data : [],
						type : 'arearange',
						lineWidth : 0,
						linkedTo : ':previous',
						color: Highcharts.getOptions().colors[0],
						fillOpacity: 0.3,
						zIndex: 0
					}],
					plotOptions : {
						series : {
							animation : false,
							tooltip : {
								headerFormat : '<b>{point.key}:</b> ',
								pointFormat : "{point.tooltip}"
							}
						},
						column : {
							color : '#aaa',
							borderRadius : 5,
							borderWidth : 2
						},
						line : {
							marker : {
								fillColor : 'white',
								lineWidth : 2,
								lineColor: Highcharts.getOptions().colors[0]
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
					}
				});
				field.formatAxis(options.yAxis);
				$scope.chartOptions = options;
			}
		}

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$('#' + $scope.settings.id + '-tab').on('shown', $scope.draw);
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
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.times = result[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length) {
				var type = $scope.settings.statistic === 'count' || $scope.settings.statistic === 'sum' ? 'column' : 'line';
				var field = Field.find($scope.settings.value_field);
				var options = {
					chart : {
						type : 'column',
						polar: true
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
						shared : true,
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
							color : 'rgba(47,126,216,0.3)',
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
				field.formatAxis(options.yAxis);
				$scope.chartOptions = options;				
			}
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$('#' + $scope.settings.id + '-tab').on('shown', $scope.draw);
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
			}
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
			$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
				$.extend($scope, options)
				$.extend($scope.settings, settings)
				$scope.update(null, result);
			});
		};
		$scope.update = function(event, result) {
			$scope.data = result[$scope.settings.id] || [];
			$timeout($scope.draw, 0); // delay for correct width
		};
		$scope.draw = function() {
			var xField = Field.find($scope.settings.field_x);
			var yField = Field.find($scope.settings.field_y);
			if ($scope.data && $scope.data.length) {
				var options = {
					chart : {
						type : 'scatter',
						zoomType: 'xy'
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
						crosshairs : [ true, true ],
						shared : false,
						hideDelay : 0,
						formatter : function() {
							return '<b>x</b>: ' + (xField.toText(this.x) || this.x) + ($scope.settings.unit_x || '') + ', ' +
								'<b>y</b>: ' + (yField.toText(this.y) || this.y) + ($scope.settings.unit_y || '');
						}
					},
					series : [{
						data : $scope.data,
						color : 'rgba(119, 152, 191, 0.5)',
						allowPointSelect : true,
						marker : {
							radius : 5
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
				if ($scope.data.length > 1 && $scope.settings.regression == 'linear') {
					var regression = statistics.regression($scope.data);
					options.series.push({
						type : 'line',
						data : regression.data,
						color : 'rgba(119, 152, 191, 1.0)',
						enableMouseTracking : false,
						marker : {
							enabled : false
						}
					});
				}
				if ($scope.data.length > 3) {
					var correlation = statistics.correlate($scope.data, true);
					$scope.rChartOptions = {
						chart : {
							type : 'line',
							inverted : true,
							height : 75,
							plotBorderWidth : 1,
							plotBackgroundColor : '#fafafa',
							marginLeft : 65
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
							data : [[ correlation.r ]],
							color : 'rgba(119, 152, 191, 1.0)',
							animation : false,
							marker : {
								radius : 5
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
							data : [[ correlation.lower, correlation.upper ]],
							lineWidth : 2,
							color : 'rgba(119, 152, 191, 1.0)',
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
		$('#' + $scope.settings.id + '-tab').on('shown', $scope.draw);
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
				google.load('maps', '3.13', { other_params : 'libraries=places&sensor=false&key=' + googleApiKey, callback : function() {
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
		$scope.init = function() {
			$scope.event = new Event($scope.selectedEvent);
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
						$scope.editEvent(null);
						delay($scope.refresh);
					})
					.error(function(response) {
						$scope.message = response.message || 'Couldn\'t create this event.';
					});
			} else {
				$http.put('/buckets/' + $scope.params.bucketId + '/' + $scope.event['@id'], $scope.event)
					.success(function(response, status, headers) {
						$scope.editEvent(null);
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
			google.load('maps', '3.13', { other_params : 'libraries=places&sensor=false&key=' + googleApiKey, callback : function() {
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
			return day + 'T' + $scope.time + '.000' + $scope.timezone;
		}
		function local(date) {
			return new Date(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
		}
		function utc(date) {
			return new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
		}

		$scope.init = function() {
			var date = new Date();
			date.setSeconds(0);
			$scope.date = utc(date);
			$scope.time = moment().format('HH:mm:ss');
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

	app.service('tasks', [ '$http', 'delay', function($http, delay) {
		this.refresh = function($scope, taskId, callback) {
			$http.get('/tasks/' + taskId)
				.success(function(response) {
					if (response.authorizationUrl) {
						$scope.alert.show('Task requires <a href="' + response.authorizationUrl + '">authorization</a>.', 'alert-block');
					} else if (response.status == 'FAILED') {
						$scope.alert.show('Couldn\'t refresh task.', 'alert-error');
					}
					if (callback) {
						delay(function() {
							callback(response);
						});
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
	}]);

	app.controller('CreateTaskDialogController', ['$scope', '$http', 'tracker', 'tasks', function($scope, $http, tracker, tasks) {
	
		$scope.types = [ 
			{ 'id' : 'fitbit', 'description' : 'Creates one step count event per day.' },
			{ 'id' : 'fitbit-intraday', 'description' : 'Creates one event for each period of time spent moving, sitting or sleeping (100 to 1,000 events per day).' },
			{ 'id' : 'bodymedia', 'description' : 'Creates one calorie count and one sleep summary event per day.' },
			{ 'id' : 'foursquare', 'description' : 'Creates an event for each Foursquare check-in.' },
			{ 'id' : 'netatmo', 'description' : 'Creates an event for each weather station measurement (up to 300 per day).' },
			{ 'id' : 'withings', 'description' : 'Creates an event for each weight measurement.' },
			{ 'id' : 'demo', 'description' : 'Creates an event with a custom tag each time this task is run.' }
		];

		$scope.init = function() {
			$scope.message = '';
			$scope.type = $scope.types[0];
			$scope.settings = { foo : 42 };
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
					$scope.closeDialog();
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					var taskId = location.replace(/.+\//, '');
					tasks.refresh($scope, taskId, function() {
						$scope.refresh();
					})
				})
				.error(function(response) {
					$scope.message = 'Couldn\'t create task. Try again later or contact support.';
				});
			tracker.event('action', 'create task');
		};
	}]);

	app.controller('FitbitSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'steps',
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FitbitIntradaySettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('BodyMediaSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('FoursquareSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};

		$scope.init();
	}]);

	app.controller('NetatmoSettingsController', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					marker : new Date(moment().utc().startOf('month').valueOf())
			};
		};	

		$scope.init();
	}]);

	app.controller('WithingsSettingsController', ['$scope', '$http', 'Field', 'googleApiKey', function($scope, $http, Field, googleApiKey) {

		$scope.init = function() {
			$scope.settings = $scope.$parent.$parent.settings = {
					tag : 'body',
					unit : 'kg',
					marker : new Date(moment().utc().startOf('month').valueOf()),
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

	app.controller('TaskAuthorizationController', ['$scope', '$http', '$routeParams', '$location', 'tasks', function($scope, $http, $routeParams, $location, tasks) {
		
		$scope.taskId = $routeParams.taskId;

		$http.post('/tasks/' + $scope.taskId, { 'credentials' : $location.search() })
			.success(function(response) {
				tasks.refresh($scope, $scope.taskId, function(task) {
					$scope.alert.show('Task is authorized.', 'alert-success');
					$location.url('/buckets/' + task.bucket);
				});
			})
			.error(function(response, status) {
				if (status < 500) {
					$scope.message = 'Can\'t authorize this task.';
				} else {
					$scope.message = 'Couldn\'t authorize this task. Try again later or contact support.';
				}
			});
	}]);

	app.controller('DocumentController', ['$scope', '$location', '$routeParams', '$timeout', function($scope, $location, $routeParams, $timeout) {
		var id = $location.path().substring(1).replace('/', '-');
		var element = document.getElementById(id);
		if (element) {
			$timeout(function() { 
				element.scrollIntoView(true);
			});
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

		var Field = function(name, icon, type, units, readOnly, toText, toHtml, formatAxis) {
			this.name = name;
			this.icon = icon;
			this.type = type;
			this.units = units;
			this.readOnly = readOnly;
			this.toText = toText;
			this.toHtml = toHtml;
			this.formatAxis = formatAxis;
		}

		Field.prototype.toNumber = function(value) {
			if (value === null) {
				return null;
			}
			if (typeof value === 'number') {
				return value;
			}
			if (typeof value === 'object' && value.hasOwnProperty('@value')) {
				return value['@value'];
			}
			return undefined;
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
				fieldOptions.formatAxis || function(options) {}
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return value != null ? moment.duration(Number(value)).countdown() : value; // some durations are stored as strings!
			},
			toHtml : function(value) {
				return '<span class="nowrap">' +
			  	'<i class="' + this.icon + '" title="Duration"></i> ' + this.toText(value) +
			  '</span>';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				return typeof value === 'object' ? value['@value'] + ' ' + value.unit : '';
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
				if (value) {
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
			return User.find(identity).getName();
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
				scope.$on(attrs.uiFocusOn, function() {
					setTimeout(function() {
						element.select();
					}, 0);
				});
			}
		};
	});

	app.directive('uiModal', function() {
		return {
			restrict : 'A',
			link : function(scope, element, attrs, model) {
				element.addClass('modal hide');
				if (attrs.uiModalClose) {
					element.on('hidden', function() {
						if (scope.$eval(attrs.uiModal)) {
							scope.$apply(attrs.uiModalClose);
						}
					});
				}
				scope.$watch(attrs.uiModal, function(value) {
					if (value) {
						if (attrs.uiModalOpen) {
							scope.$eval(attrs.uiModalOpen);
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
				var updateModel = function() {
					return $scope.$apply(function() {
						return controller.$setViewValue(element.val());
					});
				};
				var options = { 
					show24Hours : true, 
					showSeconds : true,
					spinnerImage : ''
				};
				element.timeEntry(options).change(updateModel);
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
			require : '?ngModel',
			restrict : 'A',
			scope : true,
			link : function($scope, element, attrs) {
				var defaultOptions = {
					chart : {
						renderTo : element[0]
					}
				};
				$scope.$watch(attrs.uiChartOptions, function(newOptions) {
					if (newOptions) {
						if ($scope.chart) {
							$scope.chart.destroy();
						}
						$scope.chart = new Highcharts.Chart($.extend(true, {}, newOptions, defaultOptions));
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

}());
