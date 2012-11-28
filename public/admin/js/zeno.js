(function() {
	
	'use strict';
	
	var adminApp = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);
	
	adminApp.config(function($routeProvider) {
		$routeProvider.when('/', { templateUrl: '/admin/partials/dashboard.html' });
		$routeProvider.otherwise({ templateUrl : '/partials/404.html' });
	});

	adminApp.controller('AdminCtrl', ['$scope', '$location', function($scope, $location) {
		$scope.filter = $location.search()['q'];
		$scope.setFilter = function(filter) {
			$scope.filter = filter;
			$location.search('q', $scope.filter);
		};
	}]);
	
	adminApp.controller('HistoryAdminCtrl', ['$scope', '$http', function($scope, $http) {
	
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
			$http.get('/queue/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.commands = response.commands;
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);
	
	adminApp.controller('BucketListAdminCtrl', ['$scope', '$http', function($scope, $http) {
	
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
					params.identity = $scope.filter;
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
				$scope.alert.show('Deleted a bucket.', 'alert-success', response.undo);
				$scope.reload();
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
	
	adminApp.controller('UserListAdminCtrl', ['$scope', '$http', function($scope, $http) {
	
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
				$scope.alert.show('Closed account of ' + userId + '.', 'alert-success', response.undo);
				$scope.reload();
			});
		};
	
		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);
	
	adminApp.controller('TaskListAdminCtrl', ['$scope', '$http', function($scope, $http) {
	
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
					params.field = 'principal';
					params.value = $scope.filter;
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
				$scope.alert.show('Deleted a task.', 'alert-success', response.undo);
				$scope.reload();
			});
		};
	
		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

}());
