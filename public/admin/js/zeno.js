(function() {

	'use strict';

	var app = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

	app.config(function($routeProvider) {
		$routeProvider.when('/', { templateUrl: '/admin/partials/dashboard.html' });
		$routeProvider.otherwise({ templateUrl : '/partials/404.html' });
	});

	app.controller('admin.DashboardController', ['$scope', '$location', '$http', function($scope, $location, $http) {
		$scope.constraint = $location.search()['q'];
		$scope.setConstraint = function(constraint) {
			$scope.constraint = constraint;
			$location.search('q', $scope.constraint);
		};
		$scope.refresh = function(params) {
			$http.get('/status')
				.success(function(response) {
					$scope.status = response;
				})
				.error(function(response) {
					$scope.status = { nodes : '?', health : 'UNKNOWN' };
				});
		};
		$scope.$on('reload', $scope.refresh);
		$scope.refresh();
	}]);

	app.controller('admin.QueueController', ['$scope', '$http', 'delay', function($scope, $http, delay) {
	
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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.constraint) {
				params.q = 'principal:' + $scope.constraint;
			}
			return params;
		}
		$scope.refresh = function(params) {
			$http.get('/journal/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.commands = response.commands;
			});
		};
		$scope.undo = function(commandId) {
			$scope.$parent.undo(commandId);
			delay($scope.reload);
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.BucketListController', ['$scope', '$http', 'Bucket', 'delay', 'token', function($scope, $http, Bucket, delay, token) {

		$scope.offset = 0;
		$scope.limit = 10;
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
			var params = {
					offset : $scope.offset,
					limit : $scope.limit
				};
				if ($scope.constraint) {
					params.q = 'roles.principal:' + $scope.constraint;
				}
				return params;
		}
		$scope.refresh = function(params) {
			$scope.token = token.get();
			$http.get('/buckets/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.buckets = response.buckets;
			});
		};
		$scope.remove = function(bucketId) {
			$http({ method : 'DELETE', url : '/buckets/' + bucketId }).success(function(response, code, headers) {
				delay($scope.reload);
			});
		};
		$scope.getOwner = function(bucket) {
			return new Bucket(bucket).getOwner();
		};
		$scope.isPublished = function(bucket) {
			return new Bucket(bucket).isPublished();
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.UserListController', ['$scope', '$http', 'delay', 'token', function($scope, $http, delay, token) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.users = null;

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
			$scope.token = token.get();
			if ($scope.constraint) {
				$http.get('/users/?' + $.param({ identity : $scope.constraint, detail : 1 })).success(function(response) {
					$scope.total = 1;
					$scope.users = [ response ];
				});
			} else {
				$http.get('/users/?' + $.param($.extend($scope.params(), params))).success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.users = response.users;
				});
			}
		};
		$scope.select = function(selected) {
			$scope.selected = selected;
		};
		$scope.close = function(userId) {
			$http({ method : 'DELETE', url : '/users/' + user.name }).success(function(response, code, headers) {
				delay($scope.reload);
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.EditQuotaDialogController', ['$scope', '$http', 'delay', function($scope, $http, delay) {
		$scope.init = function() {
			$scope.message = '';
			$scope.quota = $scope.selected ? $scope.selected.quota : '';
		};
		$scope.save = function() {
			$http.post('/users/' + $scope.selected.name, { 'quota' : $scope.quota })
				.success(function(response) {
					$scope.closeDialog();
					delay($scope.reload);
				})
				.error(function(response, code) {
					$scope.message = 'Failed (' + code + ')';
				});
		};
	}]);


	app.controller('admin.AuthorizationListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

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
			var params = {
				offset : $scope.offset,
				limit : $scope.limit
			};
			if ($scope.constraint) {
				params.q = 'principal:' + $scope.constraint;
			}
			return params;
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
				.success(function(response, code, headers) {
					delay($scope.reload);
				});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.TaskListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.tasks = null;

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
				if ($scope.constraint) {
					params.q = 'principal:' + $scope.constraint;
				}
				return params;
		}
		$scope.refresh = function(params) {
			$http.get('/tasks/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.tasks = response.tasks;
			});
		};
		$scope.remove = function(taskId) {
			$http({ method : 'DELETE', url : '/tasks/' + taskId }).success(function(response, code, headers) {
				delay($scope.reload);
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

}());
