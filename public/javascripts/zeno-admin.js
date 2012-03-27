angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

HistoryAdminCtrl.$inject = ['$scope', '$http'];
function HistoryAdminCtrl($scope, $http) {

	$scope.offset = 0;
	$scope.limit = 10;
	$scope.history = null;

	$scope.hasPrev = function() {
		return $scope.offset > 0;
	}
	$scope.hasNext = function() {
		return $scope.history && $scope.offset + $scope.limit < $scope.history.total;
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
			$scope.history = response;
		});
	};

	$scope.$on('reload', $scope.refresh);
	$scope.refresh({});
}

BucketListAdminCtrl.$inject = ['$scope', '$http'];
function BucketListAdminCtrl($scope, $http) {
	$scope.buckets = [ ];
	$http.get('/buckets/?identity=*').success(function(response, code) {
		$scope.buckets = response;
	});
	$scope.remove = function(bucketId) {
		$http.delete('/buckets/' + bucketId + '/').success(function(response, code, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Deleted a bucket.', 'alert-success', undo);
			$scope.reload();
		});
	};
}

UserListAdminCtrl.$inject = ['$scope', '$http'];
function UserListAdminCtrl($scope, $http) {
	$scope.users = [ ];
	$http.get('/users/?identity=*').success(function(response, code) {
		$scope.users = response;
	});
	$scope.close = function(userId) {
		$http.delete('/users/' + userId).success(function(response, code, headers) {
			var undo = headers('Undo');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Closed account of ' + userId + '.', 'alert-success', undo);
			$scope.reload();
		});
	};
}
