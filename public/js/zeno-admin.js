(function() {
	
	'use strict';
	
	var adminApp = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);
	
	adminApp.config(function($routeProvider) {
		$routeProvider.when('/', { templateUrl: '/admin/dashboard.html' });
		$routeProvider.otherwise({ templateUrl : '/404.html' });
	});
	
	
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
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
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
			return {
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
				$scope.alert.show('Deleted a bucket.', 'alert-success', response.undo);
				$scope.reload();
			});
		};
		$scope.getOwner = function(bucket) {
			for (var i = 0, max = bucket.permissions.length; i < max; ++i) {
				if (bucket.permissions[i].permission === 'ALL') {
					return bucket.permissions[i].principal;
				}
			}
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
			$http.get('/users/?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.users = response.users;
			});
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

}());
