(function() {

	'use strict';

	var app = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

	app.config(function($routeProvider) {
		$routeProvider.when('/', { templateUrl: '/admin/partials/dashboard.html' });
		$routeProvider.otherwise({ templateUrl : '/partials/404.html' });
	});

	app.controller('admin.DashboardController', ['$scope', '$location', function($scope, $location) {
		$scope.filter = $location.search()['q'];
		$scope.setFilter = function(filter) {
			$scope.filter = filter;
			$location.search('q', $scope.filter);
		};
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
			if ($scope.filter) {
				params.identity = $scope.filter;
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

	app.controller('admin.BucketListController', ['$scope', '$http', 'Bucket', 'delay', function($scope, $http, Bucket, delay) {

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
				if ($scope.filter) {
					params.q = 'roles.principal:' + $scope.filter;
				}
				return params;
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

	app.controller('admin.UserListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

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
			if ($scope.filter) {
				$http.get('/users/?' + $.param({ identity : $scope.filter, detail : 1 })).success(function(response) {
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
		$scope.close = function(userId) {
			$http({ method : 'DELETE', url : '/users/' + userId }).success(function(response, code, headers) {
				delay($scope.reload);
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
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
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
		};
		$scope.refresh = function(params) {
			if ($scope.filter) {
				$http.get('/authorizations/?' + $.param($.extend($scope.params(), params, { 'field' : 'principal', 'value' : $scope.filter }))).success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.authorizations = response.authorizations;
				});
			} else {
				$http.get('/authorizations/?' + $.param($.extend($scope.params(), params))).success(function(response) {
					$.extend($scope, params);
					$scope.total = response.total;
					$scope.authorizations = response.authorizations;
				});
			}
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
				if ($scope.filter) {
					params.q = 'principal:' + $scope.filter;
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
