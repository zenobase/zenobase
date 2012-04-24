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

app.config(['$routeProvider', function($routeProvider) {
	$routeProvider.when('/', { template: '/public/home.html' })
		.when('/buckets/:bucketId/', { template : '/public/dashboard.html', reloadOnSearch : false })
		.when('/users/:userId', { template : '/public/user.html' })
		.when('/users/:userId/reset', { template : '/public/reset.html' })
		.when('/users/:userId/verify', { template : '/public/verify.html' })
		.otherwise({ template : '/public/404.html' });
}]);

app.controller('MainCtrl', ['$scope', '$route', '$http', '$location', '$defer', function($scope, $route, $http, $location, $defer) {
	$scope.whoami = function() {
		$http.get('/who', httpConfig()).success(function(response) {
			$scope.user = response ? new User(response) : null;
		});
	};

	$scope.alert = new Alert();
	$scope.undo = function(commandId) {
		$http.post(commandId, 'undo', httpConfig()).success(function(response, code) {
			$scope.alert.clear();
			$defer(function() { window.location.reload(); }, DELAY);
		});
	};
	$scope.reload = function() {
		$route.reload();
	};
	$scope.broadcast = function(event) {
		$scope.$broadcast(event);
	};
	$scope.signOut = function() {
		$http.post('/signout', httpConfig()).success(function(response, code) {
				$scope.alert.clear();
				$scope.user = null;
				if ($location.url() == '/') {
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

var locale = {
		getTimezone : function() {
			var offset = -new Date().getTimezoneOffset();
			var result = offset < 0 ? '-' : '+';
			offset = Math.abs(offset);
			var hours = offset / 60;
			var minutes = offset % 60;
			if (hours < 10) {
				result += '0';
			}
			result += hours;
			if (minutes < 10) {
				minutes += '0';
			}
			result += minutes;
			return result;
		}
};

app.controller('UserCtrl', ['$scope', '$http', '$routeParams', function($scope, $http, $routeParams) {

	$scope.userId = $routeParams.userId;
	$scope.userInfo = null;

	if ($scope.userId !== 'guest') {
		$http.get('/users/' + $scope.userId).success(function(response) {
			$scope.userInfo = new User(response);
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
				.success(function(response, status, headers) {
					var undo = headers('Undo');
					console.assert(undo, 'missing undo header');
					$scope.alert.show('Updated user info.', 'alert-success', undo);
					$scope.reload();
				})
				.error(function(response, code) {
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

app.controller('AuthFormCtrl', ['$scope', '$http', function($scope, $http) {
	$scope.dialog = $('#sign-in-dialog');
	$scope.data = function() {
		return {
			username : $scope.username,
			password : $scope.password,
			remember : $scope.remember
		};
	};
	$scope.signIn = function() {
		$http.post('/signin', $scope.data())
			.success(function(response) {
				$scope.$parent.user = new User(response);
				$scope.dialog.modal('hide');
				$scope.clear();
				$scope.reload();
			})
			.error(function(response, code) {
				switch (code) {
					case 401:
						$scope.message = 'The username or password you entered is incorrect.';
						break;
					default:
						$scope.message = 'Unable to sign in, please try again later or contact support.';
				}
			});
	}
	$scope.clear = function() {
		$scope.username = '';
		$scope.password = '';
		$scope.remember = true;
		$scope.message = '';
	};
	$scope.clear();
	$scope.$on('event:unauthorized', function() {
		$scope.dialog.modal('show');
	});
	$scope.dialog.on('shown', function () {
		$scope.clear();
		$('#username').select();
	});
}]);

app.controller('ResetPasswordFormCtrl', ['$scope', '$http', function($scope, $http) {
	$scope.dialog = $('#reset-password-dialog');
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
				$scope.clear();
				$scope.home();
			})
			.error(function(response, code) {
				switch (code) {
					case 400:
						$scope.message = 'The username and email address you entered don\'t match our records.';
						break;
					default:
						$scope.message = 'Unable to reset your password, please try again later or contact support.';
				}				
			});
	};
	$scope.clear = function() {
		$scope.username = '';
		$scope.email = '';
		$scope.message = '';
	};
	$scope.clear();
	$scope.dialog.on('shown', function () {
		$scope.clear();
		$('#reset-username').select();
	});
}]);

app.controller('SignUpFormCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	$scope.dialog = $('#sign-up-dialog');
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
				$scope.clear();
				$scope.dialog.modal('hide');
				$location.url('/users/' + $scope.$parent.user.name);
			})
			.error(function(response, code) {
				switch (code) {
					case 409:
						$scope.message = 'The chosen username is not available.';
						break;
					default:
						$scope.message = 'Unable to sign up, please try again later or contact support.';
				}				
			});
	};
	$scope.clear = function() {
		$scope.username = '';
		$scope.password = '';
		$scope.retypedPassword = '';
		$scope.email = '';
		$scope.message = '';
	};
	$scope.clear();
	$scope.dialog.on('shown', function () {
		$scope.clear();
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
	$scope.clear = function() {
		$scope.password = '';
		$scope.retypedPassword = '';
		$scope.message = '';
	};
	$scope.clear();
}]);

app.controller('BucketListCtrl', ['$scope', '$http', function($scope, $http) {

	$scope.offset = 0;
	$scope.limit = 5;
	$scope.total = 0;
	$scope.buckets = null;

	$scope.hasPrev = function() {
		return $scope.offset > 0;
	}
	$scope.hasNext = function() {
		return $scope.offset + $scope.limit < $scope.total;
	}
	$scope.prev = function() {
		$scope.refresh({ offset : $scope.offset - $scope.limit });
	}
	$scope.next = function() {
		$scope.refresh({ offset : $scope.offset + $scope.limit });
	}
	$scope.params = function() {
		return {
			identity : $scope.userInfo['@id'],
			offset : $scope.offset,
			limit : $scope.limit
		};
	}
	$scope.refresh = function(params) {
		$http.get('/buckets/?' + $.param($.extend($scope.params(), params))).success(function(response) {
			$.extend($scope, params);
			$scope.total = response.total;
			$scope.buckets = response.buckets;
		});
	};
	$scope.remove = function(bucketId) {
		$http({ method : 'DELETE', url : '/buckets/' + bucketId }).success(function(response, code, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Deleted a bucket.', 'alert-success', undo);
			$scope.reload();
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
		$http.post('/buckets/', $scope.template).success(function(data, status, headers) {
			var location = headers('Location');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			$location.url(location);
			$scope.whoami();
		});
	}
}]);

app.controller('CreateBucketDialogCtrl', ['$scope', '$http', '$location', function($scope, $http, $location) {
	$scope.label = 'My Data';
	$scope.create = function() {
		$http.post('/buckets/', { label : $scope.label}).success(function(data, status, headers) {
			var location = headers('Location');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			$('#create-bucket-dialog').modal('hide');
			$location.url(location);
			$scope.whoami();
		});
	}
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
	for (var i = 0; i < len; ++i) {
		var pos = Math.floor(Math.random() * chars.length);
		id += chars.substring(pos, pos + 1);
	}
	return id;
}

app.controller('AddWidgetCtrl', ['$scope', '$http', '$route', '$routeParams', '$location', function($scope, $http, $route, $routeParams, $location) {

	$scope.templates = [
  	{ label : 'Timeline', description : 'Timeline with event counts.', type : 'timeline' },
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
		$scope.addWidget(settings);
		$scope.closeWidgetDialog();
	};
	$scope.cancel = function() {
		$scope.closeWidgetDialog();
	};
	$scope.findTemplates = function() {
		return $.grep($scope.templates, function(template) {
			return !template.singleton || !$scope.exists(template.template);
		});
	};
	$scope.exists = function(template) {
		if ($scope.bucket) {
			for (var i = 0; i < $scope.bucket.widgets.length; ++i) {
				if ($scope.bucket.widgets[i].template == template) {
					return true;
				}
			}
		}
		return false;
	};
}]);

app.controller('BucketCtrl', ['$scope', '$http', '$route', '$routeParams', '$location', '$defer', function($scope, $http, $route, $routeParams, $location, $defer) {

	$scope.bucketId = $routeParams.bucketId;
	$http.get('/buckets/' + $scope.bucketId).success(function(response) {
		$scope.bucket = response;
	});

	$scope.filters = [];
	$scope.widgets = [];

	$scope.getWidgetSettings = function(placement) {
		return $scope.bucket && $.grep($scope.bucket.widgets, function(widget) {
			return widget.placement == placement;
		});
	};
	$scope.removeWidget = function(id) {
		$scope.bucket.widgets = $.grep($scope.bucket.widgets, function(widget) {
			return widget.id != id;
		});
		$scope.widgets = $.grep($scope.widgets, function(widget) {
			return widget.settings.id != id;
		});
	};
	$scope.placement = null;
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
		return '/public/dashboard/' + type + '.html';
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
		$http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'w' : w }, true)).success(callback);
	};
	$scope.refresh = function() {
		$scope.updateFilters();
		var params = $.map($scope.widgets, function(widget) { return widget.params(); });
		$scope.search(params, function(response) {
			$scope.total = response.total;
			$scope.$broadcast('result', response);
		});
	};
	$scope.params = function() {
		return null;
	};
	$scope.remove = function(eventId) {
		$http({ method : 'DELETE', url : '/buckets/' + $scope.bucketId + '/' + eventId }).success(function(response, status, headers) {
			$defer($scope.refresh, DELAY);
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Deleted an event.', 'alert-success', undo);
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
}]);

app.controller('BucketFormCtrl', ['$scope', '$http', function($scope, $http) {
	$scope.save = function(settings) {
		$http.post('/buckets/' + $scope.bucketId, $scope.bucket).success(function (response, status, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Saved settings.', 'alert-success', undo);
			++$scope.$parent.bucket.version;
			$scope.$parent.cancel();
		});
	};
	$scope.cancel = function() {
		$scope.$parent.cancel();
		$scope.reload();
	};
}]);

app.controller('EventListCtrl', ['$scope', function($scope) {

	$scope.offset = 0;
	$scope.total = 0;
	$scope.items = [];

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
		$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
			$.extend($scope, options);
			$.extend($scope.settings, settings);
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		$scope.total = result.total;
		$scope.items = result[$scope.settings.id];
	};

	$scope.dialogShown = false;
	$scope.showDialog = function(dialogShown) {
		$scope.dialogShown = dialogShown;
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

app.controller('EventListConfigCtrl', ['$scope', function($scope) {
	$scope.limit = $scope.settings.limit;
	$scope.save = function() {
		$scope.refresh({ offset : 0 }, { limit : $scope.limit });
		$('#event-list-config-dialog').modal('hide');
	};
}]);

app.controller('TermCountCtrl', ['$scope', function($scope) {

	$scope.offset = 0;
	$scope.more = false;
	$scope.terms = [];

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
		$scope.refresh({ offset : 0 }, { order : order, reverse : order == $scope.settings.order && !$scope.settings.reverse });
	}
	$scope.getClasses = function(column) {
		var classes = [];
		if (column == $scope.order) {
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
		$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
			$.extend($scope, options)
			$.extend($scope.settings, settings)
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		var terms = result[$scope.settings.id];
		$scope.more = terms.length > $scope.settings.limit;
		$scope.terms = terms.slice(0, $scope.settings.limit);
	};
	$scope.filter = function(term) {
		$scope.offset = 0;
		$scope.addFilter(new Filter($scope.settings.field, term.label))
	};

	$scope.dialogShown = false;
	$scope.showDialog = function(dialogShown) {
		$scope.dialogShown = dialogShown;
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

app.controller('WidgetSettingsCtrl', ['$scope', function($scope) {
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
	$scope.reset();
}]);

app.controller('TermGanttCtrl', ['$scope', function($scope) {

	$scope.terms = [];

	$scope.params = function() {
		return { 
			id : $scope.settings.id,
			type : 'gantt',
			termField : $scope.settings.termField, 
			timeField : $scope.settings.timeField,
			timezone : locale.getTimezone(),
			order : $scope.settings.order,
			limit : $scope.settings.limit
		};
	};
	$scope.refresh = function(options, settings) {
		$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
			$.extend($scope, options)
			$.extend($scope.settings, settings)
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		$scope.terms = result[$scope.settings.id];
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

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

app.controller('RatingCountCtrl', ['$scope', function($scope) {

	$scope.field = 'rating';
	$scope.from = 10;
	$scope.to = 90;
	$scope.step = 20;
	$scope.ratings = [];

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
		$scope.ratings = result[$scope.settings.id];
	};
	$scope.refresh = function(options, settings) {
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

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

app.controller('ScoreboardCtrl', ['$scope', function($scope) {

	$scope.terms = [];

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
		$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
			$.extend($scope, options)
			$.extend($scope.settings, settings)
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		$scope.terms = result[$scope.settings.id];
	};
	$scope.filter = function(term) {
		$scope.addFilter(new Filter($scope.settings.termField, term.label))
	};

	$scope.dialogShown = false;
	$scope.showDialog = function(dialogShown) {
		$scope.dialogShown = dialogShown;
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

/**
 * @constructor
 */
function Interval(name, pattern) {
	this.name = name;
	this.pattern = pattern;
}

Interval.prototype.zoomIn = function() {
	for (var i = 0; i < Interval.VALUES.length; ++i) {
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
	for (var i = 0; i < Interval.VALUES.length; ++i) {
		if (Interval.VALUES[i].pattern === value.length) {
			return Interval.VALUES[i];
		}
	}
};

app.controller('TimelineCtrl', ['$scope', function($scope) {

	$scope.field = 'timestamp';
	$scope.times = [];

	$scope.params = function() {
		$scope.interval = Interval.VALUES[1];
		$scope.range = '';
		$.each($scope.getFilters($scope.field), function(i, filter) {
			$scope.interval = Interval.match(filter.value);
			$scope.range = filter.value;
		});
		return $scope.interval && { 
			id : $scope.settings.id,
			type : 'timeline',
			field : $scope.field, 
			interval : $scope.interval.name,
			range : $scope.range,
			timezone : locale.getTimezone()
		};
	};
	$scope.refresh = function(options, settings) {
		$scope.search([ $.extend($scope.params(), options, settings) ], function(result) {
			$.extend($scope, options)
			$.extend($scope.settings, settings)
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		$scope.times = result[$scope.settings.id];
		$scope.draw($scope);
	};
	$scope.draw = function($scope) {
		if ($scope.times && $scope.times.length) {
			google.load("visualization", "1", { packages : [ "corechart" ], callback : function() { 
				var data = new google.visualization.DataTable();
				data.addColumn('string', $scope.interval.name);
				data.addColumn('number', 'Count');
				$.each($scope.times, function(i, time) {
					data.addRow([ time.label, time.count ]);
				});
				var options = {
					height : 100,
					legend : { position : 'none' },
					series : [ { color : 'gray' } ],
					chartArea : { width: '100%', left: 0 },
					vAxis : { gridlines : { color : '#EEE' }, baselineColor : '#EEE' },
					hAxis : { baselineColor : 'white', textPosition : 'none' } 
				};
				var chart = new google.visualization.ColumnChart(document.getElementById('timeline-' + $scope.settings.id));
				chart.draw(data, options);
				google.visualization.events.addListener(chart, 'select', function() {
					var selection = chart.getSelection();
					var value = data.getValue(selection[0].row, 0);
					$scope.interval = $scope.interval.zoomIn();
					$scope.addFilter(new Filter($scope.field, value), true);
					$scope.refresh();
				});
			}});
		}
	}

	$scope.dialogShown = false;
	$scope.showDialog = function(dialogShown) {
		$scope.dialogShown = dialogShown;
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);

app.controller('MapCtrl', ['$scope', function($scope) {

	$scope.field = 'location';
	$scope.points = null;
	$scope.map = null;

	$scope.refresh = function(options, settings) {
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
				for (var i = 0; i < value.length; ++i) {
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
		google.maps.event.addDomListener(control, 'click', function() {
			$scope.filterBounds();
			$scope.refresh();
		});
		return parent;
	}

	$scope.dialogShown = false;
	$scope.showDialog = function(dialogShown) {
		$scope.dialogShown = dialogShown;
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}]);


app.controller('TemplateCtrl', ['$scope', '$http', '$defer', '$routeParams', function($scope, $http, $defer, $routeParams) {
	$scope.dialog = $('#create-event-dialog');
	$scope.content = '';
	$scope.params = $routeParams;
	$scope.i = 0;
	$scope.templates = [
		undefined,
		{
			tag : [ "lunch", "pizza" ],
			location : {
				lat : 47.62,
				lon : -122.35
			},
			rating : 80
		},
		{
			tag : [ "sleep" ]
		},
		{
			tag : [ "movie" ],
			rating : 40,
			resource : {
				title : "Citizen Kane",
				url : "http://www.imdb.com/title/tt0033467/"
			}
		},
		{
			tag : [ "hike" ],
			location : {
				lat : 60.57,
				lon : -151.25
			},
			distance : {
				'@value' : 10,
				unit : 'km'
			},
			height : {
				'@value' : 1500,
				unit : 'm'
			}
		},
		{
			random : 1000
		}
	];
	$scope.create = function() {
		$http.post('/buckets/' + $scope.params.bucketId + '/', $scope.content).success(function(response, status, headers) {
			var location = headers('Location');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			$defer(function() {
				$scope.reload();
				$scope.dialog.modal('hide');
			}, DELAY);
		});
	}
	$scope.getTemplate = function(i) {
		return JSON.stringify($scope.templates[i], null, ' ');
	}
}]);

/**
 * @constructor
 */
function Field(name, icon, format) {
	this.name = name;
	this.icon = icon;
	this.format = format;
}

Field.FIELDS = [];
Field.FIELDS_BY_NAME = {};

Field.register = function(field) {
	Field.FIELDS.push(field); 
	Field.FIELDS_BY_NAME[field.name] = field; 
}

Field.register(new Field('tag', 'icon-tag', function(value) { 
	return '<span class="nowrap" title="Tag">' +
		'<i class="' + this.icon + '"></i> ' + encode(value) +
  '</span>';
}));

Field.register(new Field('resource', 'icon-bookmark', function(value) { 
	return '<span title="Resource">' +
  	'<i class="' + this.icon + '"></i>&nbsp;' +
  	'<a href="' +  encode(value.url) + '" rel="nofollow">' +  encode(value.title) + '</a>' +
  '</span>';
}));

Field.register(new Field('distance', 'icon-resize-horizontal', function(value) { 
	return '<span class="nowrap" title="Distance">' +
  	'<i class="' + this.icon + '"></i> ' + Math.round(value['@value']) + value.unit +
  '</span>';
}));

Field.register(new Field('height', 'icon-resize-vertical', function(value) { 
	return '<span class="nowrap" title="Height">' +
  	'<i class="' + this.icon + '"></i>' + Math.round(value['@value']) + 'm' +
  '</span>';
}));

Field.register(new Field('location', 'icon-map-marker', function(value) { 
	return '<span class="nowrap" title="Location">' +
		'<i class="' + this.icon + '"></i> ' +
		'<a href="http://maps.google.com/maps?q=' + 
			encode(value.lat + ',' + value.lon) + '&t=p&z=5">' + 
			encode(value.lat + ', ' + value.lon) + '</a>' +
	'</span>';
}));

Field.register(new Field('timestamp', 'icon-calendar', function(value) { 
	return '<span class="nowrap">' +
  	'<i class="' + this.icon + '" title="Timestamp"></i>' +
		'<abbr title="' + value + '"> ' + humane.date(new Date(Date.parse(value))) + '</abbr>' +
  '</span>';
}));

Field.register(new Field('duration', 'icon-time', function(value) { 
	return '<span class="nowrap">' +
  	'<i class="' + this.icon + '" title="Duration"></i> ' + humane.duration(value, false) +
  '</span>';
}));

Field.register(new Field('rating', 'icon-star', function(value) { 
	var stars = Math.round((value || 0) / 20);
	var html = '<span class="nowrap" title="Rated ' + stars + '/5">';
	for (var i = 0; i < 5; ++i) {
		html += '<i class="' + (stars > i ? 'icon-star' : 'icon-star-empty') + '"></i>';
	}
	html += '</span>';
	return html;
}));

Field.register(new Field('author', 'icon-user', function(value) { 
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

app.filter('fields', function() {
	return function(event) {
		var html = '';
		$.each(Field.findAll(), function(i, field) {
			var value = event[field.name];
			if (value) {
				if (i > 0) {
					html += ' &nbsp; ';
				}
				if ($.isArray(value)) {
					$.each(value, function(i, value) {
						if (i > 0) {
							html += ' &nbsp; ';
						}
						html += field.format(value);
					});
				}
				else {
						html += field.format(value);
				}
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
			if (response.status == 401) {
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
				var start = attrs.copyrightYear;
				var year = new Date().getFullYear();
				var text = start == year ?
					start : start + '&ndash;' + year;
				element.html(text);
			};
		}
	};
});

function encode(value) {
	return $('<div />').text(value).html();
}

function defined(a, b) {
	return a !== undefined ? a : b;	
}

function httpConfig() {
	return { headers : { 'Content-Type' : 'application/x-www-form-urlencoded' } };
}
