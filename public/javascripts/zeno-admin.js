angular.module('ZenoAdminModule', [ 'ZenoModule' ]);

HistoryCtrl.$inject = ['$scope', '$http'];
function HistoryCtrl($scope, $http) {

	$scope.offset = 0;
	$scope.limit = 10;
	$scope.history = [];

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

	$scope.refresh({});
}
