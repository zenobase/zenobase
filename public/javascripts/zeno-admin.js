var adminApp = angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

adminApp.config(function($routeProvider) {
	$routeProvider.when('/', { template: '/public/admin/dashboard.html' });
	$routeProvider.otherwise({ redirectTo : '/' });
});


HistoryAdminCtrl.$inject = ['$scope', '$http'];
function HistoryAdminCtrl($scope, $http) {

	$scope.offset = 0;
	$scope.limit = 5;
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
}

BucketListAdminCtrl.$inject = ['$scope', '$http'];
function BucketListAdminCtrl($scope, $http) {

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
		$http.delete('/buckets/' + bucketId + '/').success(function(response, code, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Deleted a bucket.', 'alert-success', undo);
			$scope.reload();
		});
	};
	$scope.getOwner = function(bucket) {
		for (var i = 0; i < bucket.roles.length; ++i) {
			if (bucket.roles[i].role === 'owner') {
				return bucket.roles[i].identity;
			}
		}
	};

	$scope.$on('reload', $scope.refresh);
	$scope.refresh({});
}

UserListAdminCtrl.$inject = ['$scope', '$http'];
function UserListAdminCtrl($scope, $http) {

	$scope.offset = 0;
	$scope.limit = 5;
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
		$http.delete('/users/' + userId).success(function(response, code, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Closed account of ' + userId + '.', 'alert-success', undo);
			$scope.reload();
		});
	};

	$scope.$on('reload', $scope.refresh);
	$scope.refresh({});
}
