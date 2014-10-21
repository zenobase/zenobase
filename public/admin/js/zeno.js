(function() {

	'use strict';

	var app = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

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
			var m = stackLines[callerIndex].match(/\-([0-9a-f]+).js:/);
			if (!m) {
				throw new Error('missing version');
			}
			return m[1];
		}
	})();

	app.config(function($routeProvider) {
		$routeProvider.when('/', { templateUrl: '/admin/partials/dashboard-' + version + '.html' });
		$routeProvider.otherwise({ templateUrl : '/partials/404-' + version + '.html' });
	});

	app.controller('admin.DashboardController', ['$scope', '$location', '$http', function($scope, $location, $http) {
		$scope.constraint = $location.search()['q'];
		$scope.setConstraint = function(constraint) {
			$scope.constraint = constraint;
			$location.search('q', $scope.constraint);
		};
		$scope.refreshAll = function() {
			$scope.$broadcast('refreshAll');
		};
		$scope.refresh = function() {
			$http.get('/status')
				.success(function(response) {
					$scope.status = response;
				})
				.error(function(response) {
					$scope.status = { nodes : '?', health : 'UNKNOWN' };
				});
		};
		$scope.setReadOnly = function(readOnly) {
			$http.post('/status', { read_only : readOnly })
				.then(function() {
					$scope.refresh();
				});
		};
		$scope.$on('refreshAll', function() {
			$scope.refresh();
		});
		$scope.refresh();
	}]);

	app.controller('admin.JournalController', ['$scope', '$http', 'delay', function($scope, $http, delay) {
	
		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.commands = null;

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
				offset : $scope.offset,
				limit : $scope.limit
			};
		}
		$scope.refresh = function(params) {
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/journal/' : '/journal/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.commands = response.commands;
			});
		};
		$scope.undo = function(commandId) {
			$scope.$parent.undo(commandId);
			delay($scope.refreshAll);
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.refresh({});
	}]);

	app.controller('admin.BucketListController', ['$scope', '$http', 'Bucket', 'delay', 'token', function($scope, $http, Bucket, delay, token) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.buckets = null;
		$scope.events = 0;

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
				offset : $scope.offset,
				limit : $scope.limit
			};
		}
		var path = function(resource) {
			return $scope.constraint ? '/users/' + $scope.constraint + resource : resource;
		}
		$scope.refresh = function(params) {
			$scope.token = token.get();
			$http.get(path('/buckets/') + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.buckets = response.buckets;
			});
			$http.get(path('/events/')).success(function(response) {
				$scope.events = response.total;
			});
		};
		$scope.remove = function(bucketId) {
			$http({ method : 'DELETE', url : '/buckets/' + bucketId }).success(function(response, code, headers) {
				delay($scope.refreshAll);
			});
		};
		$scope.getOwner = function(bucket) {
			return new Bucket(bucket).getOwner();
		};
		$scope.isPublished = function(bucket) {
			return new Bucket(bucket).isPublished();
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.refresh({});
	}]);

	app.controller('admin.UserListController', ['$scope', '$http', '$q', 'delay', 'token', function($scope, $http, $q, delay, token) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.users = null;
		$scope.filter = null;

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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.filter) {
				params.q = $scope.filter;
			}
			return params;
		};
		$scope.refresh = function(params) {
			$scope.token = token.get();
			if ($scope.constraint) {
				$http.get('/users/' + $scope.constraint).success(function(response) {
					if (response.name) {
						$scope.total = 1;
						$scope.users = [ response ];
					} else {
						$scope.total = 0;
						$scope.users = [];
					}
				});
			} else {
				$http.get('/users/?' + $.param($.extend($scope.params(), params))).success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.users = response.users;
				});
			}
		};
		$scope.suspend = function(username) {
			$http.post('/users/@' + username, { 'suspended' : true }).success(function() {
				delay($scope.refreshAll);
			});
		};
		$scope.reverify = function(user) {
			$http.post('/users/@' + user.name, { 'email' : user.email }).success(function() {
				delay($scope.refreshAll);
			});
		};
		$scope.remove = function(username) {
			$http({ method : 'DELETE', url : '/users/@' + username }).success(function() {
				delay($scope.refreshAll);
			});
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.$watch('filter', function(to, from) {
			if (from !== to) {
				$scope.refresh({ offset : 0 });
			}
		});
		$scope.refresh({});
	}]);

	app.controller('admin.EditQuotaDialogController', ['$scope', '$http', 'delay', function($scope, $http, delay) {
		$scope.init = function(user) {
			$scope.user = user;
			$scope.message = '';
			$scope.quota = user.quota;
		};
		$scope.save = function() {
			$http.post('/users/@' + $scope.user.name, { 'quota' : $.isNumeric($scope.quota) ? $scope.quota : null })
				.success(function(response) {
					$scope.closeDialog();
					delay($scope.refreshAll);
				});
		};
	}]);

	app.controller('admin.AuthorizationListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.authorizations = null;
		$scope.filter = null;

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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.filter) {
				params.q = $scope.filter;
			}
			return params;
		};
		$scope.refresh = function(params) {
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/authorizations/' : '/authorizations/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params)))
				.success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.authorizations = response.authorizations;
				});
		};
		$scope.remove = function(authId) {
			$http({ method : 'DELETE', url : '/authorizations/' + authId })
				.success(function(response, code, headers) {
					delay($scope.refreshAll);
				});
		};
		$scope.removeExpired = function(bucketId) {
			$http({ method : 'DELETE', url : '/authorizations/' }).success(function(response, status) {
				console.assert(status === 204, status);
				delay($scope.refreshAll);
			});
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.$watch('filter', function(to, from) {
			if (from !== to) {
				$scope.refresh({ offset : 0 });
			}
		});
		$scope.refresh({});
	}]);

	app.controller('admin.CredentialsListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.credentials = null;
		$scope.filter = null;

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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.filter) {
				params.q = $scope.filter;
			}
			return params;
		}
		$scope.refresh = function(params) {
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/credentials/' : '/credentials/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.credentials = response.items;
			});
		};
		$scope.remove = function(credentialsId) {
			$http({ method : 'DELETE', url : '/credentials/' + credentialsId }).success(function(response, code, headers) {
				delay($scope.refreshAll);
			});
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.$watch('filter', function(to, from) {
			if (from !== to) {
				$scope.refresh({ offset : 0 });
			}
		});
		$scope.refresh({});
	}]);

	app.controller('admin.TaskListController', ['$scope', '$http', 'delay', 'taskRunner', function($scope, $http, delay, taskRunner) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.tasks = null;
		$scope.running = {};
		$scope.filter = null;

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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.filter) {
				params.q = $scope.filter;
			}
			return params;
		}
		$scope.refresh = function(params) {
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/tasks/' : '/tasks/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.tasks = response.tasks;
			});
		};
		$scope.run = function(taskId) {
			$scope.running[taskId] = true;
			$scope.alert.clear();
			taskRunner.runOne($scope, taskId)['finally'](function() {
				delay(function() {
					$scope.refresh({});
				});
				delete $scope.running[taskId];
			});
		};
		$scope.remove = function(taskId) {
			$http({ method : 'DELETE', url : '/tasks/' + taskId }).success(function(response, code, headers) {
				delay($scope.refreshAll);
			});
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.$watch('filter', function(to, from) {
			if (from !== to) {
				$scope.refresh({ offset : 0 });
			}
		});
		$scope.refresh({});
	}]);

	app.controller('admin.SnapshotController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.snapshots = null;
		$scope.snapshotting = false;

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
				offset : $scope.offset,
				limit : $scope.limit
			};
		}
		$scope.refresh = function(params) {
			$http.get('/snapshots/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.snapshots = response.snapshots;
			});
		};
		$scope.remove = function(snapshotId) {
			$http({ method : 'DELETE', url : '/snapshots/' + snapshotId }).success(function(response, code, headers) {
				delay($scope.refreshAll);
			});
		};
		$scope.snapshot = function() {
			$scope.snapshotting = true;
			$http({ method : 'POST', url : '/snapshots/' }).then(function() {
				delay(function() {
					$scope.snapshotting = false;
					$scope.refreshAll();
				});
			});
		};

		$scope.$on('refreshAll', function() {
			$scope.refresh({ offset : 0 });
		});
		$scope.refresh({});
	}]);

}());
