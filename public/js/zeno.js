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

	var DELAY = 1000; // ms after which we assume changes will be visible

	var VERSION = function() {
		var meta = document.getElementsByTagName('meta');
		for (var i = 0; i < meta.length; ++i) {
			if (meta[i].getAttribute('property') == 'version') {
				return meta[i].content;
			}
		}
		throw new Error("missing version");
	}();

	var versioned = function(path) {
		return path.replace(/\.(.+)$/, '-' + VERSION + '.$1');
	};

	app.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/', { templateUrl: versioned('/partials/home.html') })
			.when('/buckets/:bucketId/', { templateUrl : versioned('/partials/dashboard.html'), reloadOnSearch : false })
			.when('/users/:userId', { templateUrl : versioned('/partials/user.html') })
			.when('/users/:userId/reset', { templateUrl : versioned('/partials/reset.html') })
			.when('/users/:userId/verify', { templateUrl : versioned('/partials/verify.html') })
			.otherwise({ templateUrl : versioned('/partials/404.html') });
	}]);

	app.controller('MainCtrl', ['$scope', '$route', '$http', '$location', '$timeout', function($scope, $route, $http, $location, $timeout) {
		$scope.whoami = function() {
			$http.get('/who').success(function(response) {
				$scope.user = response ? new User(response) : null;
			});
		};
	
		$scope.alert = new Alert();
		$scope.undo = function(commandId) {
			$http.post('/queue/' , { 'undo' : commandId })
				.success(function(response, code) {
					$scope.alert.clear();
					$timeout(function() { window.location.reload(); }, DELAY);
				})
				.error(function(response) {
					$scope.alert.show('Couldn\'t undo.');
				});
		};
		$scope.reload = function() {
			$route.reload();
		};
		$scope.broadcast = function(event) {
			$scope.$broadcast(event);
		};
		$scope.signOut = function() {
			_gaq.push([ '_trackEvent', 'session', 'sign out', $scope.user.getName() ]);
			$http.post('/signout', { 'username' : $scope.user.getName() }).success(function(response, code) {
					$scope.alert.clear();
					$scope.user = null;
					if ($location.url() === '/') {
						$scope.reload();
					} else {
						$scope.home();
					}
			});
		};
		$scope.home = function() {
			$location.url('/');
		};
		$scope.whoami();
	}]);
	
	/**
	 * @constructor
	 */
	function Alert() {
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
	
	User.CACHE = {};
	
	/**
	 * @constructor
	 */
	function User(data) {
		$.extend(this, data);
		User.CACHE[this['@id']] = this;
	}
	
	User.prototype.getName = function() {
		return this.name || 'guest';
	};
	
	User.find = function(identity) {
		if (User.CACHE[identity]) {
			return User.CACHE[identity];
		}
		var user;
		$.ajax('/users/?identity=' + identity, { async : false, success : function(response) {
			user = new User(response);
		}});
		return user;
	};
	
	/**
	 * @constructor
	 */
	function Filter(field, value) {
		this.field = field;
		this.value = value;
	}
	
	Filter.SEPARATOR = ':';
	
	Filter.prototype.toString = function() {
		return this.field + Filter.SEPARATOR + this.value;
	};
	
	Filter.parse = function(s) {
		var pos = s.indexOf(Filter.SEPARATOR);
		if (pos < 1 || pos > s.length - 1) {
			throw "Can't parse filter: " + s;
		}
		var field = s.substring(0, pos);
		var value = s.substring(pos + 1);
		return new Filter(field, value);
	}
		
	app.controller('UserCtrl', ['$scope', '$http', '$routeParams', function($scope, $http, $routeParams) {
	
		$scope.userId = $routeParams.userId;
		$scope.userInfo = null;
	
		if ($scope.userId !== 'guest') {
			$http.get('/users/' + $scope.userId)
				.success(function(response) {
					$scope.userInfo = new User(response);
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
				$http({ method : 'DELETE', url : '/users/' + $routeParams.userId }).success(function(response) {
					$scope.signOut();
				});
			}
		};
	}]);
	
	app.controller('UserFormCtrl', ['$scope', '$http', function($scope, $http) {
	
		$scope.editing = false;

		$scope.data = function() {
			var data = {};
			if ($scope.email && $scope.email !== $scope.userInfo.email || !$scope.userInfo.verified) {
				data.email = $scope.email;
			}
			return data;
		};
		$scope.save = function() {
			var data = $scope.data();
			if (!$.isEmptyObject(data)) { 
				$http.post('/users/' + $scope.userInfo.name, data)
					.success(function(response) {
						$scope.alert.show('Updated user info.', 'alert-success', response.undo);
						$scope.reload();
					})
					.error(function(response) {
						$scope.message = 'Update failed. Try again later or contact support.';
					});
			} else {
				$scope.cancel();
			}
		};
		$scope.cancel = function() {
			$scope.editing = false;
		};
		$scope.$on('edit:user', function() {
			$scope.message = '';
			$scope.email = $scope.userInfo.email;
			$scope.editing = true;		
		});
	}]);
	
	app.controller('AuthFormCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {

		$scope.dialog = $('#sign-in-dialog');

		$scope.init = function() {
			$scope.username = '';
			$scope.password = '';
			$scope.remember = true;
			$scope.message = '';
		};
		$scope.data = function() {
			return {
				username : $scope.username,
				password : $scope.password,
				remember : $scope.remember
			};
		};
		$scope.signIn = function() {
			_gaq.push([ '_trackEvent', 'session', 'sign in', $scope.username ]);
			$http.post('/signin', $scope.data())
				.success(function(response) {
					$scope.$parent.user = new User(response);
					$scope.dialog.modal('hide');
					if ($location.url() === '/') {
						$location.url('/users/' + $scope.username);
					} else {
						$scope.reload();
					}
				})
				.error(function(response, code) {
					if (code === 401) {
						$scope.message = 'The username or password you entered is incorrect.';
					} else {
						$scope.message = 'Unable to sign in, please try again later or contact support.';
					}
				});
		}

		$scope.init();
		$scope.$on('event:unauthorized', function() {
			$scope.dialog.modal('show');
		});
		$scope.dialog.on('shown', function () {
			$scope.$apply($scope.init);
			$('#username').select();
		});
	}]);
	
	app.controller('ResetPasswordFormCtrl', ['$scope', '$http', function($scope, $http) {

		$scope.dialog = $('#reset-password-dialog');

		$scope.init = function() {
			$scope.username = '';
			$scope.email = '';
			$scope.message = '';
		};
		$scope.data = function() {
			return {
				username : $scope.username,
				email : $scope.email
			};
		};
		$scope.requestReset = function() {
			$http.post('/reset', $scope.data())
				.success(function(response) {
					$scope.alert.show('A password reset request has been sent by email. Check your inbox.');
					$scope.dialog.modal('hide');
					$scope.home();
				})
				.error(function(response, code) {
					if (code === 400) {
						$scope.message = 'The username and email address you entered don\'t match our records.';
					} else {
						$scope.message = 'Unable to reset your password, please try again later or contact support.';
					}
				});
		};

		$scope.init();
		$scope.dialog.on('shown', function () {
			$scope.$apply($scope.init);
			$('#reset-username').select();
		});
	}]);
	
	app.controller('SignUpFormCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {

		$scope.dialog = $('#sign-up-dialog');

		$scope.init = function() {
			$scope.username = '';
			$scope.password = '';
			$scope.retypedPassword = '';
			$scope.email = '';
			$scope.message = '';
		};
		$scope.data = function() {
			return {
				'username' : $scope.username,
				'password' : $scope.password,
				'email' : $scope.email
			};
		};
		$scope.submit = function() {
			if ($scope.password !== $scope.retypedPassword) {
				$scope.message = 'Passwords don\'t match.';
				return;
			}
			$http.post('/users/', $scope.data())
				.success(function(response, code) {
					$scope.$parent.user = new User(response);
					$scope.dialog.modal('hide');
					$location.url('/users/' + $scope.$parent.user.name);
				})
				.error(function(response, code) {
					if (code === 409) {
						$scope.message = 'The chosen username is not available.';
					} else {
						$scope.message = 'Unable to sign up, please try again later or contact support.';
					}
				});
		};

		$scope.init();
		$scope.dialog.on('shown', function () {
			$scope.$apply($scope.init);
			$('#sign-up-username').select();
		});
	}]);
	
	app.controller('VerifyCtrl', ['$scope', '$http', '$location', '$routeParams', function($scope, $http, $location, $routeParams) {
		$http.post('/users/' + $routeParams.userId, { 'key' : $location.search()['key'], 'verified' : true })
			.success(function(response) {
				$scope.alert.show('Your email address has been verified.', 'alert-success');
				$location.url('/users/' + $routeParams.userId);
			})
			.error(function(response) {
				$scope.alert.show('Your email address could not be verified.', 'alert-error');
				$location.url('/users/' + $routeParams.userId);
			});
	}]);
	
	app.controller('ResetPasswordCtrl', ['$scope', '$http', '$location', '$routeParams', function($scope, $http, $location, $routeParams) {

		var userId = $routeParams.userId;
		var key = $location.search()['key'];
		var expires = $location.search()['expires'];

		$scope.init = function() {
			$scope.password = '';
			$scope.retypedPassword = '';
			$scope.message = '';
		};
		$scope.submit = function() {
			if ($scope.password !== $scope.retypedPassword) {
				$scope.message = 'Passwords don\'t match.';
				return;
			}
			$http.post('/users/' + userId, { 'key' : key, 'expires' : expires, 'password' : $scope.password })
				.success(function(response) {
					$scope.alert.show('Your password has been changed.', 'alert-success');
					$location.url('/users/' + userId);
					$scope.whoami();
				})
				.error(function(response) {
					$scope.alert.show('Your password could not be changed.', 'alert-error');
				});		
		};

		$scope.init();
	}]);
	
	app.controller('BucketListCtrl', ['$scope', '$http', function($scope, $http) {
	
		$scope.offset = 0;
		$scope.limit = 5;
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
				identity : $scope.userInfo['@id'],
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			$http.get('/buckets/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.buckets = response.buckets;
			});
		};
		$scope.remove = function(bucketId) {
			$http({ method : 'DELETE', url : '/buckets/' + bucketId })
				.success(function(response) {
					$scope.alert.show('Deleted a bucket.', 'alert-success', response.undo);
					$scope.reload();
				})
				.error(function(response) {
					$scope.alert.show('Couldn\'t delete the bucket.', 'alert-error');
				});
		};
	
		$scope.$watch('userInfo', function(user) {
			if (user) {
				$scope.refresh({});
			}
		});
		$scope.$on('reload', $scope.refresh);
	}]);
	
	app.controller('HomeCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
		$scope.template = {
			label : 'My Data'
		};
		$scope.create = function() {
			_gaq.push([ '_trackEvent', 'home', 'create', $scope.template.label ]);
			$http.post('/buckets/', $scope.template)
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$location.url(location);
					$scope.whoami();
				})
				.error(function(response) {
					$scope.alert.show('Couldn\'t create a new bucket.', 'alert-error');					
				});
		};
	}]);
	
	app.controller('CreateBucketDialogCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {

		$scope.dialog = $('#create-bucket-dialog');

		$scope.init = function() {
			$scope.label = 'My Data';
			$scope.message = '';
		};
		$scope.create = function() {
			$http.post('/buckets/', { label : $scope.label})
				.success(function(response, status, headers) {
					var location = headers('Location');
					console.assert(status === 201, status);
					console.assert(location, 'missing location header');
					$scope.dialog.modal('hide');
					$location.url(location);
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.message = 'Can\'t create a new bucket with this label.';					
					} else {
						$scope.message = 'Couldn\'t create a new bucket. Please try agan later or contact support.';					
					}
				});
		};

		$scope.init();
		$scope.dialog.on('shown', function () {
			$scope.$apply($scope.init);
			$('#bucket-label-field').select();
		});
	}]);
	
	/**
	 * @constructor
	 */
	function WidgetParams() {
		this.params = [];
	}
	
	WidgetParams.prototype.add = function(params) {
		this.params.push();
	}; 
	
	function randomID() {
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
	
	app.controller('AddWidgetCtrl', ['$scope', '$http', '$route', '$routeParams', '$location', function($scope, $http, $route, $routeParams, $location) {
	
		$scope.templates = [
	  	{ label : 'Timeline', description : 'Timeline with event counts.', type : 'timeline', valueField : 'timestamp', statistic : 'count' },
	  	{ label : 'Map', description : 'Map with event locations.', type : 'map', singleton : true },
	  	{ label : 'List', description : 'List with the most recent events.', type : 'list', singleton : true, limit : 5, order : 'timestamp', reverse : false },
	  	{ label : 'Count', description : 'Counts events for each value in a field.', type : 'count', field : 'tag', order : 'count', reverse : false, limit : 5 },
	  	{ label : 'Date Range', description : 'First and last occurence of each value in a field.', type : 'gantt', termField : 'tag', timeField : 'timestamp', order : 'max', limit : 10 },
	  	{ label : 'Ratings', description : 'Counts events by their rating.', type : 'histogram' },
	  	{ label : 'Scoreboard', description : 'Statistics for the values in a field', type : 'scoreboard', termField : 'author', valueField : 'distance', unit : 'km', order : 'total', limit : 10 }                    
	  ];
		$scope.template = null;
		$scope.add = function() {
			var settings = { id : randomID(), placement : $scope.placement };
			$.extend(settings, $scope.template);
			delete settings.description;
			$scope.addWidget(settings);
			$scope.closeWidgetDialog();
		};
		$scope.cancel = function() {
			$scope.closeWidgetDialog();
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

	function Bucket(data) {
		$.extend(this, data);
	}
		
	app.controller('BucketCtrl', ['$scope', '$http', '$route', '$routeParams', '$location', '$timeout', function($scope, $http, $route, $routeParams, $location, $timeout) {
	
		function updateEditable() {
			if ($scope.user) {
				for (var i = 0; i < $scope.bucket.permissions.length; ++i) {
					if ($scope.bucket.permissions[i].principal === $scope.user['@id']) {
						$scope.editable = $scope.bucket.permissions[i].permission === 'ALL';
						break;
					}
				}
			}
		} 

		$scope.bucketId = $routeParams.bucketId;
		$http.get('/buckets/' + $scope.bucketId)
			.success(function(response) {
				$scope.bucket = new Bucket(response);
				$scope.$watch('user', updateEditable);
			})
			.error(function(response, status) {
				if (status < 500) {
					$scope.message = 'Can\'t retrieve this bucket.';
				} else {
					$scope.message = 'Couldn\'t retrieve this bucket. Try again later or contact support.';
				}
			});
	
		$scope.filters = [];
		$scope.widgets = [];
	
		$scope.getWidgetSettings = function(placement) {
			return $scope.bucket && $.grep($scope.bucket.widgets, function(widget) {
				return widget.placement === placement;
			});
		};
		$scope.removeWidget = function(id) {
			$scope.bucket.widgets = $.grep($scope.bucket.widgets, function(widget) {
				return widget.id !== id;
			});
			$scope.widgets = $.grep($scope.widgets, function(widget) {
				return widget.settings.id !== id;
			});
		};
		$scope.placement = null;
		$scope.canImport = function() {
			return typeof FileReader != 'undefined' && $scope.editable;
		};
		$scope.chooseWidget = function(placement) {
			$scope.placement = placement;
		};
		$scope.closeWidgetDialog = function() {
			$scope.placement = null;
		};
		$scope.addWidget = function(settings) {
			$scope.bucket.widgets.push(settings);
		};
		$scope.getTemplate = function(type) {
			return versioned('/dashboard/' + type + '.html');
		};
		$scope.register = function(widget) {
			$scope.widgets.push(widget);
			if ($scope.widgets.length === $scope.bucket.widgets.length) {
				$scope.refresh();
			}
		};
	
		$scope.search = function(params, callback) {
			var q = $scope.filters;
			var w = $.map(params, function(param) {
				return $.map(param, function(value, key) { return key + ':' + value }).join(',');
			});
			$http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'w' : w }, true))
				.success(callback)
				.error(function(response) { callback({ total : -1 }) });
		};
		$scope.refresh = function() {
			$scope.updateFilters();
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
		$scope.editEvent = function(event) {
			$scope.selectedEvent = event;
		};
		$scope.removeEvent = function(eventId) {
			$http({ method : 'DELETE', url : '/buckets/' + $scope.bucketId + '/' + eventId }).success(function(response, status, headers) {
				$timeout($scope.refresh, DELAY);
				$scope.alert.show('Deleted an event.', 'alert-success', response.undo);
			});
		};
	
		$scope.$on('$routeUpdate', function() {
			$scope.refresh();
		});
		$scope.updateFilters = function() {
			var q = $location.search()['q'];
			$scope.filters = q ? $.map(q.split('__'), function(s) { return Filter.parse(s) }) : [ ];
		};
		$scope.getFilters = function(field) {
			return $.grep($scope.filters, function(filter) {
				return filter.field === field;
			});
		};
		$scope.containsFilter = function(filter) {
			return $.grep($scope.filters, function(f) {
				return angular.equals(f, filter);
			}).length > 0;
		};
		$scope.addFilter = function(filter, replace) {
			if ($scope.containsFilter(filter)) {
				return;
			}
			if (replace) {
				$scope.filters = $.grep($scope.filters, function(f) {
					return f.field !== filter.field;
				});
			}
			$scope.filters.push(filter);
			$location.search('q', $scope.filters.join('__'));
		};
		$scope.removeFilter = function(filter) {
			$scope.filters = $.grep($scope.filters, function(f) {
				return !angular.equals(f, filter);
			});
			$location.search('q', $scope.filters.length ? $scope.filters.join('__') : null);
		};
		$scope.getFilterIcon = function(filter) {
			var field = Field.find(filter.field);
			return field ? field.icon : 'icon-ban-circle';
		};
	
		$scope.editing = false;
		$scope.edit = function() {
			$scope.editing = true;
		};
		$scope.cancel = function() {
			$scope.editing = false;
		};
		$scope.isPublished = function() {
			return $scope.bucket && $.grep($scope.bucket.permissions, function(permission) {
				return permission.principal === '*';
			}).length > 0;
		};
	}]);
	
	app.controller('BucketFormCtrl', ['$scope', '$http', function($scope, $http) {
		$scope.publish = function() {
			if (!$scope.isPublished()) {
				$scope.bucket.permissions.push({ 'principal' : '*', 'permission' : 'USE' });
			}
		};
		$scope.unpublish = function() {
			$scope.bucket.permissions = $.grep($scope.bucket.permissions, function(permission) {
				return permission.principal !== '*';
			});
		};
		$scope.save = function(settings) {
			$http.post('/buckets/' + $scope.bucketId, $scope.bucket)
				.success(function (response, status, headers) {
					$scope.alert.show('Saved settings.', 'alert-success', response.undo);
					++$scope.$parent.bucket.version;
					$scope.$parent.cancel();
				})
				.error(function(response, status) {
					if (status === 400) {
						$scope.alert.show('Can\'t save this bucket', 'alert-error');
					} else {
						$scope.alert.show('Couldn\'t save this bucket. Try again later or contact support.', 'alert-error');						
					}
				});
		};
		$scope.cancel = function() {
			$scope.$parent.cancel();
			$scope.alert.clear();
			$scope.reload();
		};
	}]);
	
	app.controller('EventListCtrl', ['$scope', function($scope) {
	
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
				offset : $scope.offset, 
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
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);
	
	app.controller('TermCountCtrl', ['$scope', function($scope) {
	
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
			$scope.addFilter(new Filter($scope.settings.field, term.label))
		};
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);
	
	var WidgetSettingsCtrl = function($scope) {
		$scope.save = function() {
			$scope.refresh({ offset : 0 }, $scope.settings);
			$scope.showDialog(false);
		};
		$scope.cancel = function() {
			$scope.showDialog(false);
			$scope.reset();
		};
		$scope.reset = function() {
			$scope.settings = angular.copy($scope.$parent.settings);
		};
		$scope.getField = function(name) {
			return Field.find(name);
		};
		$scope.reset();
	};
	
	app.controller('WidgetSettingsCtrl', ['$scope', function($scope) {
		WidgetSettingsCtrl($scope);
	}]);
	
	app.controller('TermGanttCtrl', ['$scope', function($scope) {
	
		$scope.init = function() {
			$scope.terms = null;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'gantt',
				termField : $scope.settings.termField, 
				timeField : $scope.settings.timeField,
				timezone : new Date().getTimezone(),
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
					term.freq = Math.round((new Date(term.last).getTime() - new Date(term.first).getTime()) / term.count);
				});
			}
		};
		$scope.filter = function(term) {
			$scope.addFilter(new Filter($scope.settings.termField, term.label))
		};
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);
	
	app.controller('RatingCountCtrl', ['$scope', function($scope) {
	
		$scope.field = 'rating';
		$scope.from = 10;
		$scope.to = 90;
		$scope.step = 20;

		$scope.init = function() {
			$scope.ratings = null;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'histogram',
				field : $scope.field, 
				from : $scope.from,
				to : $scope.to,
				step : $scope.step
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
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);
	
	app.controller('ScoreboardCtrl', ['$scope', function($scope) {
	
		$scope.init = function() {
			$scope.terms = null;
		};
		$scope.params = function() {
			return { 
				id : $scope.settings.id,
				type : 'scoreboard',
				termField : $scope.settings.termField, 
				valueField : $scope.settings.valueField,
				unit : $scope.settings.unit,
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
		};
		$scope.filter = function(term) {
			$scope.addFilter(new Filter($scope.settings.termField, term.label))
		};
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
	}]);

	app.controller('ScoreboardSettingsCtrl', ['$scope', function($scope) {

		WidgetSettingsCtrl($scope);

		function isUnitValid() {
			return $.grep($scope.getUnits(), function(unit) {
				return unit === $scope.settings.unit;
			}).length > 0;
		};

		$scope.getTermFields = function() {
			return Field.findTokenFields();
		};
		$scope.getValueFields = function() {
			return Field.findUnitFields();
		};
		$scope.getUnits = function() {
			return Field.find($scope.settings.valueField).units;
		};
		$scope.$watch('settings.valueField', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
		});
	}]);
	
	/**
	 * @constructor
	 */
	function Interval(name, pattern) {
		this.name = name;
		this.pattern = pattern;
	}
	
	Interval.prototype.zoomIn = function() {
		var i, max;
		for (i = 0, max = Interval.VALUES.length; i < max; ++i) {
			if (Interval.VALUES[i].pattern > this.pattern) {
				return Interval.VALUES[i];
			}
		}
	};
	
	Interval.VALUES = [
		new Interval('year', 0),
		new Interval('month', 10), 
		new Interval('day', 13), 
		new Interval('hour', 16), 
		new Interval('minute', 18),
		new Interval('second', 21)
	];
	
	Interval.match = function(value) {
		var i, max;
		for (i = 0, max = Interval.VALUES.length; i < max; ++i) {
			if (Interval.VALUES[i].pattern === value.length) {
				return Interval.VALUES[i];
			}
		}
	};
	
	app.controller('TimelineCtrl', ['$scope', function($scope) {
	
		$scope.keyField = 'timestamp';
		$scope.init = function() {
			$scope.times = null;
		};
		$scope.params = function() {
			$scope.interval = Interval.VALUES[1];
			$scope.range = '';
			$.each($scope.getFilters($scope.keyField), function(i, filter) {
				$scope.interval = Interval.match(filter.value);
				$scope.range = filter.value;
			});
			return $scope.interval && { 
				id : $scope.settings.id,
				type : 'timeline',
				keyField : $scope.keyField, 
				valueField : $scope.settings.valueField || $scope.keyField,
				unit : $scope.settings.unit || '',
				interval : $scope.interval.name,
				range : $scope.range,
				timezone : new Date().getTimezone()
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
			$scope.draw();
		};
		$scope.draw = function() {
			if ($scope.times && $scope.times.length) {
				google.load("visualization", "1", { packages : [ "corechart" ], callback : function() { 
					var data = new google.visualization.DataTable();
					data.addColumn('string', $scope.interval.name);
					data.addColumn('number', 'Count');
					data.addColumn({ type : 'string', role : 'tooltip'});
					$.each($scope.times, function(i, time) {
						var value = time[$scope.settings.statistic || 'count'];
						var unit = '';
						if (typeof value == 'object') {
							unit = value.unit;
							value = value['@value'];
						} else {
							unit = value == 1 ? 'event' : 'events';
						}
						data.addRow([ time.label, value, time.label + ': ' + value + ' ' + unit ]);
					});
					var options = {
						height : 100,
						legend : { position : 'none' },
						series : [ { color : 'gray' } ],
						chartArea : { width : '100%', height : 90, left : 25, top : 5 },
						vAxis : { gridlines : { color : '#EEE', count : 2 }, minorGridlines : { color : '#EEE', count : 1 }, baselineColor : '#EEE', textStyle : { fontSize: 10 } },
						hAxis : { baselineColor : 'white', textPosition : 'none', textStyle : { fontSize: 10 } },
					};
					var chart = new google.visualization.ColumnChart(document.getElementById('timeline-' + $scope.settings.id));
					chart.draw(data, options);
					google.visualization.events.addListener(chart, 'select', function() {
						var selection = chart.getSelection();
						var value = data.getValue(selection[0].row, 0);
						$scope.interval = $scope.interval.zoomIn();
						$scope.$apply(function() {
							$scope.addFilter(new Filter($scope.keyField, value), true);
						});
					});
				}});
			}
		}
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};
	
		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$(window).resize($scope.draw);
		$('#' + $scope.settings.id + '-tab').on('show', $scope.draw);
	}]);

	app.controller('TimelineSettingsCtrl', ['$scope', function($scope) {

		WidgetSettingsCtrl($scope);

		function isUnitValid() {
			if ($scope.settings.valueField === $scope.keyField) {
				return $scope.settings.unit === null;
			}
			return $.grep($scope.getUnits(), function(unit) {
				return $scope.settings.unit === unit;
			}).length > 0;
		};
		function isStatisticValid() {
			return $.grep($scope.getStatistics($scope.settings.valueField), function(statistic) {
				return $scope.settings.statistic === statistic;
			}).length > 0;
		};

		$scope.getValueFields = function() {
			var fields = Field.findUnitFields();
			fields.unshift(new Field($scope.keyField));
			$scope.findUnitFields = function() {
				return fields;
			};
			return fields;
		};
		$scope.getStatistics = function(field) {
			return field === $scope.keyField ? [ 'count' ] : [ 'sum', 'avg', 'min', 'max' ];
		};
		$scope.getUnits = function() {
			return Field.find($scope.settings.valueField).units || [];
		};
		$scope.valid = function() {
			return isUnitValid() && isStatisticValid();
		};

		$scope.$watch('settings.valueField', function() {
			if (!isUnitValid()) {
				$scope.settings.unit = null;
			}
			if (!isStatisticValid()) {
				$scope.settings.statistic = $scope.getStatistics($scope.settings.valueField)[0];
			}
		});
	}]);
	
	app.controller('MapCtrl', ['$scope', function($scope) {
	
		$scope.field = 'location';
	
		$scope.init = function() {
			$scope.points = null;
			$scope.map = null;
		};
		$scope.refresh = function(options, settings) {
			$scope.init();
			$.extend($scope, options)
			$.extend($scope.settings, settings)
			$scope.$parent.refresh();
		};
		$scope.update = function(event, result) {
			var points = [ ];
			$scope.events = $.each($scope.getEvents(result), function(i, event) {
				var location = event[$scope.field];
				if ($.isArray(location)) {
					$.each(location, function(i, l) {
						points.push(l);
					});
				} else if (location) {
					points.push(location);
				}
			});
			$scope.points = points;
			$scope.draw();
		};
		$scope.filterBounds = function() {
			$scope.addFilter(new Filter($scope.field, $scope.map.getBounds().toUrlValue(2)), true);
		};
		$scope.getEvents = function(result) {
			for (var key in result) {
				var value = result[key];
				if ($.isArray(value)) {
					for (var i = 0, max = value.length; i < max; ++i) {
						if (value[i][$scope.field]) {
							return value;
						}
					}
				}
			}
			return [];
		};
		$scope.draw = function() {
			if ($scope.points.length) {
				google.load("maps", "3.8", { other_params : 'sensor=false', callback : function() {
					var options = {
						mapTypeId: google.maps.MapTypeId.TERRAIN,
						streetViewControl: false,
						mapTypeControlOptions : {
							style : google.maps.MapTypeControlStyle.DROPDOWN_MENU
						}
					};
					$scope.map = new google.maps.Map(document.getElementById('map'), options);
					var bounds = new google.maps.LatLngBounds();
					$.each($scope.points, function(i, point) {
						var latLng = new google.maps.LatLng(point.lat, point.lon);
						var marker = new google.maps.Marker({
							position : latLng, 
							map : $scope.map,
							title : 'Event: ' + latLng
						});
						bounds.extend(latLng);
					});
					if (bounds.getNorthEast().equals(bounds.getSouthWest())) {
						$scope.map.setCenter(bounds.getCenter());
						$scope.map.setZoom(2);
					} else {
						$scope.map.fitBounds(bounds);
					}
				  $scope.map.controls[google.maps.ControlPosition.TOP_RIGHT].push($scope.createFilterControl());
				}});
			} else {
				$('#map').html('<i class="none">None</i>');
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
	
		$scope.dialogShown = false;
		$scope.showDialog = function(dialogShown) {
			$scope.dialogShown = dialogShown;
		};

		$scope.init();
		$scope.register($scope);
		$scope.$on('result', $scope.update);
		$scope.$on('refresh', $scope.init);
		$('#' + $scope.settings.id + '-tab').on('show', $scope.draw);
	}]);
	
	
	app.controller('CreateEventDialogCtrl', ['$scope', '$http', '$timeout', '$routeParams', function($scope, $http, $timeout, $routeParams) {

		$scope.params = $routeParams;
		$scope.fields = Field.findEditableFields();
		$scope.init = function() {
			$scope.event = $.extend(true, {}, $scope.selectedEvent);
			$scope.entries = flatten($scope.event);
			$scope.isNew = $scope.isEmpty();
			$scope.message = '';
			$scope.field = Field.find('tag');
			$scope.value = '';
		};
		$scope.isEmpty = function() {
			return $.isEmptyObject($scope.entries);
		};
		$scope.getTemplate = function(field) {
			return field ? '/create-' + field.name + '.html' : null;
		};
		$scope.save = function() {
			if ($scope.isNew) {
				$http.post('/buckets/' + $scope.params.bucketId + '/', $scope.event)
				.success(function(response) {
					$timeout(function() {
						$scope.editEvent(null);
						$scope.reload();
					}, DELAY);
				})
				.error(function(response) {
					$scope.message = 'Couldn\'t create this event.';
				});
			} else {
				$http.post('/buckets/' + $scope.params.bucketId + '/' + $scope.event['@id'], $scope.event)
				.success(function(response) {
					$timeout(function() {
						$scope.alert.show('Updated an event.', 'alert-success', response.undo);
						$scope.editEvent(null);
						$scope.reload();
					}, DELAY);
				})
				.error(function(response) {
					$scope.message = 'Couldn\'t update this event.';
				});
			}
		};
		function flatten(event) {
			var entries = [];
			$.each($scope.fields, function(i, field) {
				var value = event[field.name];
				if (value) {
					$.each($.isArray(value) ? value : [ value ], function(i, value) {
						entries.push({ field : field, value : value });
					});
				}
			});
			return entries;
		};
		$scope.remove = function(entry) {
			var values = $scope.event[entry.field.name];
			if (values) {
				values = $.grep(values, function(value) {
					return value !== entry.value;
				});
				if (values.length > 0) {
					$scope.event[entry.field.name] = values;
				} else {
					delete $scope.event[entry.field.name];
				}
			}
		};

		$scope.$watch('selectedEvent', $scope.init);
		$scope.$watch('event', function(event) {
			$scope.entries = flatten(event);
		}, true);
	}]);
	
	
	app.controller('CreateTagFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = '';
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			var value = $scope.value;
			$scope.event[$scope.field.name].push(value);
			$scope.init();
		};
		$scope.valid = function() {
			return $scope.value;
		};

		$scope.init();
	}]);
	
	app.controller('CreateLocationFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			google.load("maps", "3.8", { other_params : 'sensor=false', callback : function() {
				var center = new google.maps.LatLng(0, 0);
				$scope.setValue(center);
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
				$scope.marker = new google.maps.Marker({
					position : center, 
					map : $scope.map,
					title : 'Location',
					draggable: true
				});
				google.maps.event.addListener($scope.map, 'click', function(e) {
					$scope.moveMarker(e.latLng);
			  });
				google.maps.event.addListener($scope.marker, 'dragend', function() {
			    $scope.setValue($scope.marker.getPosition());
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
			$scope.setValue(latLng);
			$scope.marker.setPosition(latLng);
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
			return $scope.value.lat >= -90 && $scope.value.lat <= 90 && 
				$scope.value.lon >= -180 && $scope.value.lon <= 180;
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			$scope.event[$scope.field.name].push($scope.value);
		};

		$scope.init();
	}]);
	

	app.controller('CreateTimestampFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = new Date().toTimezoneISOString();
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			$scope.event[$scope.field.name].push($scope.value);
		};
		$scope.valid = function() {
			return Date.parse($scope.value);
		};

		$scope.init();
	}]);
	
	app.controller('CreateDurationFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.days = $scope.hours = $scope.minutes = $scope.seconds = 0;
		};
		$scope.millis = function() {
			return ((($scope.days * 24 + $scope.hours) * 60 + $scope.minutes) * 60 + $scope.seconds) * 1000;
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			$scope.event[$scope.field.name].push($scope.millis());
			$scope.init();
		};
		$scope.valid = function() {
			return $scope.millis() > 0;
		};

		$scope.init();
	}]);
	
	app.controller('CreateResourceFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = {};
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			$scope.event[$scope.field.name].push($scope.value);
			$scope.init();
		};
		$scope.valid = function() {
			return $scope.value.url && $scope.value.title;
		};

		$scope.init();
	}]);
	
	app.controller('CreateUnitFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = {};
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			$scope.event[$scope.field.name].push($scope.value);
			$scope.init();
		};
		$scope.getUnits = function() {
			return $scope.field.units;
		};
		$scope.valid = function() {
			return $.isNumeric($scope.value['@value']) && $scope.value.unit;
		};

		$scope.init();
	}]);
	
	app.controller('CreateIntegerFieldCtrl', ['$scope', function($scope) {

		$scope.init = function() {
			$scope.value = 0;
		};
		$scope.addField = function() {
			if (!$scope.event[$scope.field.name]) {
				$scope.event[$scope.field.name] = [];
			}
			var value = $scope.value;
			$scope.event[$scope.field.name].push(value);
			$scope.init();
		};
		$scope.valid = function() {
			return /^\d+$/.test($scope.value);
		};

		$scope.init();
	}]);
	
	
	app.controller('ImportEventsCtrl', ['$scope', '$http', '$timeout', '$routeParams', function($scope, $http, $timeout, $routeParams) {

		$scope.dialog = $('#import-events-dialog');
		$scope.params = $routeParams;
		$scope.init = function() {
			$scope.events = [];
		};
		$scope.isEmpty = function() {
			return $scope.events.length == 0;
		};
		$scope.setFiles = function(files) {
			$scope.$apply(function(scope) {
				var reader = new FileReader();
				reader.onload = function(e) {
					scope.$apply(function(scope) {
						scope.events = JSON.parse(e.target.result);
					});
				}
				reader.readAsText(files[0]);
			});
		};
		$scope.import = function() {
			$http.post('/buckets/' + $scope.params.bucketId + '/', { 'events' : $scope.events })
				.success(function(response) {
					$timeout(function() {
						$scope.reload();
						$scope.dialog.modal('hide');
						$scope.alert.show('Imported events.', 'alert-success', response.undo);
					}, DELAY);
				})
				.error(function(response) {
					$scope.message = 'Couldn\'t import events.';
				});
		};
		$scope.init();
		$scope.dialog.on('shown', function () {
			$scope.$apply($scope.init);
		});
	}]);
	
	/**
	 * @constructor
	 */
	function Field(name, icon, units, format) {
		this.name = name;
		this.icon = icon;
		this.units = units;
		this.format = format;
	}
	
	Field.FIELDS = [];
	Field.FIELDS_BY_NAME = {};
	
	
	Field.encode = function(value) {
		return $('<div />').text(value).html();
	}
	
	Field.register = function(field) {
		Field.FIELDS.push(field); 
		Field.FIELDS_BY_NAME[field.name] = field; 
	}

	Field.register(new Field('tag', 'icon-tag', null, function(value) { 
		return '<span class="nowrap" title="Tag">' +
			'<i class="' + this.icon + '"></i> ' + Field.encode(value) +
	  '</span>';
	}));
	
	Field.register(new Field('resource', 'icon-bookmark', null, function(value) { 
		return '<span title="Resource">' +
	  	'<i class="' + this.icon + '"></i>&nbsp;' +
	  	'<a href="' +  Field.encode(value.url) + '" rel="nofollow">' +  Field.encode(value.title) + '</a>' +
	  '</span>';
	}));
	
	Field.register(new Field('distance', 'icon-resize-horizontal', [ 'mi', 'ft', 'in', 'km', 'm', 'cm', 'mm' ], function(value) { 
		return '<span class="nowrap" title="Distance">' +
	  	'<i class="' + this.icon + '"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('height', 'icon-resize-vertical', [ 'mi', 'ft', 'in', 'km', 'm', 'cm', 'mm' ], function(value) { 
		return '<span class="nowrap" title="Height">' +
	  	'<i class="' + this.icon + '"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('weight', 'icon-lock', [ 'lb', 'oz', 'kg', 'g', 'mg' ], function(value) { 
		return '<span class="nowrap" title="Weight">' +
	  	'<i class="' + this.icon + '"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('pressure', 'icon-fullscreen', [ 'Pa', 'mmHg', 'inHg', 'psi' ], function(value) { 
		return '<span class="nowrap" title="Pressure">' +
	  	'<i class="' + this.icon + '"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('location', 'icon-map-marker', null, function(value) { 
		return '<span class="nowrap" title="Location">' +
			'<i class="' + this.icon + '"></i> ' +
			'<a href="http://maps.google.com/maps?q=' + 
				Field.encode(value.lat + ',' + value.lon) + '&t=p&z=5">' + 
				Field.encode(Math.round(value.lat * 1000) / 1000 + ', ' + Math.round(value.lon * 1000) / 1000) + '</a>' +
		'</span>';
	}));
	
	Field.register(new Field('timestamp', 'icon-calendar', null, function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="Timestamp"></i> ' +
			'<abbr title="' + value + '">' + humane.date(new Date(Date.parse(value))) + '</abbr>' +
	  '</span>';
	}));
	
	Field.register(new Field('duration', 'icon-time', null, function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="Duration"></i> ' + humane.duration(value, false) +
	  '</span>';
	}));
	
	Field.register(new Field('frequency', 'icon-heart', [ 'bpm', 'Hz' ], function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="Frequency"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('count', 'icon-th', null, function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="Count"></i> ' + value +
	  '</span>';
	}));
	
	Field.register(new Field('temperature', 'icon-fire', [ 'C', 'F', 'K' ], function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="Temperature"></i> ' + value['@value'] + ' ' + value.unit +
	  '</span>';
	}));
	
	Field.register(new Field('rating', 'icon-star', null, function(value) { 
		var stars = Math.round((value || 0) / 20);
		var html = '<span class="nowrap" title="Rated ' + stars + '/5">';
		for (var i = 0; i < 5; ++i) {
			html += '<i class="' + (stars > i ? 'icon-star' : 'icon-star-empty') + '"></i>';
		}
		html += '</span>';
		return html;
	}));
	
	Field.register(new Field('author', 'icon-user', null, function(value) { 
		return '<span class="nowrap">' +
	  	'<i class="' + this.icon + '" title="User"></i> ' + User.find(value).getName() +
	  '</span>';
	}));
	
	Field.find = function(name) {
		return Field.FIELDS_BY_NAME[name];
	}
	
	Field.findAll = function() {
		return Field.FIELDS;
	}

	Field.findEditableFields = function() {
		return $.grep(Field.FIELDS, function(field) {
			return field.name !== 'author';
		});
	}

	Field.findUnitFields = function() {
		return $.grep(Field.FIELDS, function(field) {
			return field.units !== null;
		});
	}

	Field.findTokenFields = function() {
		return [ Field.find('tag'), Field.find('author') ];
	}
	
	app.filter('fields', function() {
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
						html += field.format(value);
						++count;
					});
				}
			});
			return html;
		}
	});
	
	app.filter('field', function() {
		return function(value, fieldName) {
			var field = Field.find(fieldName);
			console.assert(field, "Don't know how to format field: " + fieldName)
			return field.format(value);
		}
	});
	
	app.filter('age', function() {
		return function(date) {
			return humane.date(new Date(Date.parse(date)));
		}
	});
	
	app.filter('duration', function() {
		return function(millis) {
			return humane.duration(millis, true);
		}
	});
	
	app.filter('stars', function() {
		var field = Field.find('rating');
		return function(rating) {
			return field.format(rating);
		}
	});
	
	app.filter('username', function() {
		return function(identity) {
			return User.find(identity).getName();
		}
	});
	
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
	
	app.directive('copyrightYear', function() {
		return {
			restrict: 'A',
			compile: function() {
				return function(scope, element, attrs) {
					var start = parseInt(attrs.copyrightYear, 10);
					var year = new Date().getFullYear();
					var text = start === year ?
						start : start + '&ndash;' + year;
					element.html(text);
				};
			}
		};
	});

}());
