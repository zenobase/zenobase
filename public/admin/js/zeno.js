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
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
		}
		$scope.refresh = function(params) {
			$scope.token = token.get();
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/buckets/' : '/buckets/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
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

	app.controller('admin.UserListController', ['$scope', '$http', '$q', 'delay', 'token', function($scope, $http, $q, delay, token) {

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
				delay($scope.reload);
			});
		};
		$scope.remove = function(username) {
			$http({ method : 'DELETE', url : '/users/@' + username }).success(function() {
				delay($scope.reload);
			});
		};
		$scope.setQuotas = function(quota) {
			$http.get('/users/?q=-quota:*|created:(*..1M]&limit=1000').success(function(response) {
				var requests = [];
				$.each(response.users, function(i, user) {
					requests.push($http.post('/users/@' + user.name, { 'quota' : quota }));
				});
				$q.all(requests).then(function() {
					delay($scope.reload());
				});
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.EditQuotaDialogController', ['$scope', '$http', 'delay', function($scope, $http, delay) {
		$scope.init = function(user) {
			$scope.user = user;
			$scope.message = '';
			$scope.quota = user.quota;
		};
		$scope.save = function() {
			$http.post('/users/@' + $scope.user.name, { 'quota' : $scope.quota })
				.success(function(response) {
					$scope.closeDialog();
					delay($scope.reload);
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
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
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
					delay($scope.reload);
				});
		};
		$scope.removeExpired = function(bucketId) {
			$http({ method : 'DELETE', url : '/authorizations/' }).success(function(response, status) {
				console.assert(status === 204, status);
				delay($scope.reload);
			});
		};

		$scope.$on('reload', $scope.refresh);
		$scope.refresh({});
	}]);

	app.controller('admin.CredentialsListController', ['$scope', '$http', 'delay', function($scope, $http, delay) {

		$scope.offset = 0;
		$scope.limit = 10;
		$scope.total = 0;
		$scope.credentials = null;

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
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/credentials/' : '/credentials/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
				$.extend($scope, params);
				$scope.total = response.total;
				$scope.credentials = response.items;
			});
		};
		$scope.remove = function(credentialsId) {
			$http({ method : 'DELETE', url : '/credentials/' + credentialsId }).success(function(response, code, headers) {
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
			return {
				offset : $scope.offset,
				limit : $scope.limit
			};
		}
		$scope.refresh = function(params) {
			var path = $scope.constraint ? '/users/' + $scope.constraint + '/tasks/' : '/tasks/';
			$http.get(path + '?' + $.param($.extend($scope.params(), params))).success(function(response) {
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
