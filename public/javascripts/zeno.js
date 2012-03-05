MainCtrl.$inject = ['$route', '$http', '$location'];
function MainCtrl($route, $http, $location) {
	var $scope = this;
	$scope.whoami = function() {
		$http.get('/who', httpConfig()).success(function(response, code) {
			$scope.user = response;
		});
	};
	$scope.username = function() {
		if (!$scope.user) {
			return null;
		}
		if (!$scope.user.hasOwnProperty('name')) {
			return 'guest';
		}
		return $scope.user.name;
	};

	$scope.alert = new Alert();
	$scope.undo = function(commandId) {
		$http.post(commandId, 'undo', httpConfig()).success(function(response, code) {
			$scope.alert.clear();
			$location.url('/');
			$scope.reload();
		});
	};
	$route.when('/', { template: '/public/dashboard.html' });
	$route.when('/buckets/:bucketId/', { template: '/public/bucket.html' });
	$route.when('/buckets/:bucketId/:eventId', { template: '/public/event.html' });
	$route.when('/terms', { template: '/public/terms.html' });
	$route.when('/privacy', { template: '/public/privacy.html' });
	$route.otherwise({ redirectTo: '/' });
	$route.parent(this);
	$scope.reload = function() {
		$route.reload();
	};
	$scope.signOut = function() {
		$http.post('/signout', httpConfig()).success(function(response, code) {
				$scope.user = null;
				$scope.alert.show('Signed out.', 'alert-success');
				if ($location.url() == '/') {
					$scope.reload();
				} else {
					home();
				}
		});
	};
	$scope.home = function() {
		$location.url('/');
	};
	$scope.whoami();
}

function Alert() {
	this.clear();
}

Alert.prototype = {
	show : function(message, level, undo) {
		this.message = message;
		this.level = level;
		this.undo = undo;
	},
	clear : function() {
		this.message = '';
		this.level = 'hide';
		this.undo = '';
	}
}

function User(name) {
	this.name = name;
}

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

AuthFormCtrl.$inject = ['$http'];
function AuthFormCtrl($http) {
	var $scope = this;
	$scope.username = '';
	$scope.username = '';
	$scope.password = '';
	$scope.remember = false;
	$scope.signIn = function() {
		$http.post('/signin', $.param({ username : $scope.username, password : $scope.password, remember : $scope.remember }), httpConfig()).success(function(response, code) {
			$scope.$parent.user = new User($scope.username);
			$scope.username = '';
			$scope.password = '';
			$scope.alert.clear();
			$('#sign-in-dialog').modal('hide');
			$scope.reload();
		});
	}
	$scope.$on('event:unauthorized', function() {
		$('#sign-in-dialog').modal('show');
	});
}

SignUpFormCtrl.$inject = ['$http'];
function SignUpFormCtrl($http) {
	var $scope = this;
	$scope.username = '';
	$scope.password = '';
	$scope.passwordRepeat = '';
	$scope.email = '';
	$scope.isValid = function(field) {
		return $scope.username == 'guest' ? 'error' : 'success'; 
	};
	$scope.submit = function() {
		$http.post('/signup', $.param({ username : $scope.username, password : $scope.password, email : $scope.email, remember : true }), httpConfig()).success(function(response, code) {
			$scope.$parent.user = response;
			$scope.username = '';
			$scope.password = '';
			$scope.passwordRepeat = '';
			$scope.email = '';
			$scope.alert.clear();
			$('#sign-up-dialog').modal('hide');
			$scope.reload();
		});
	};
}

HistoryCtrl.$inject = ['$http'];
function HistoryCtrl($http) {

	var $scope = this;
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

BucketListCtrl.$inject = ['$http'];
function BucketListCtrl($http) {
	var $scope = this;
	$scope.buckets = [ ];
	$http.get('/buckets/').success(function(response, code) {
		$scope.buckets = response;
	});
	$scope.remove = function(bucketId) {
		$http.delete('/buckets/' + bucketId + '/').success(function(response, code) {
			$scope.reload();
		});
	};
}

CreateBucketDialogCtrl.$inject = ['$http', '$location'];
function CreateBucketDialogCtrl($http, $location) {
	var $scope = this;
	$scope.label = 'My Data';
	$scope.create = function() {
		$http.post('/buckets/', { label : $scope.label}).success(function(data, status, headers) {
			var location = headers('Location');
			var undo = headers('Undo');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			console.assert(undo, 'missing undo header');
			$scope.alert.show('Created a new bucket.', 'alert-success', undo);
			$('#create-bucket-dialog').modal('hide');
			$location.url(location);
			$scope.whoami();
		});
	}
}

function WidgetParams() {
	this.params = [];
}

WidgetParams.prototype.add = function(params) {
	this.params.push();
}; 

BucketCtrl.$inject = ['$http', '$routeParams', '$location'];
function BucketCtrl($http, $routeParams, $location) {

	var $scope = this;
	$scope.bucketId = $routeParams.bucketId;

	$scope.widgets = [];
	$scope.register = function(widget) {
		$scope.widgets.push(widget);
	};
	$scope.search = function(params, callback) {
		var q = $scope.filters;
		var w = $.map(params, function(param) {
			return $.map(param, function(value, key) { return key + ':' + value }).join(',');
		});
		$http.get('/buckets/' + $scope.bucketId + '/?' + $.param({ 'q' : q, 'w' : w }, true)).success(callback);
	};
	$scope.refresh = function() {
		var params = $.map($scope.widgets, function(widget) { return widget.params(); });
		$scope.search(params, function(response) {
			$scope.bucket = response;
			$scope.$broadcast('result', response);
		});
	};
	$scope.params = function() {
		return null;
	};

	var q = $location.search()['q'];
	$scope.filters = q ? $.map(q.split('__'), function(s) { return Filter.parse(s) }) : [ ];
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

	$scope.$evalAsync($scope.refresh);
}

function EventListCtrl() {

	var $scope = this;
	$scope.id = 'events';
	$scope.offset = 0;
	$scope.limit = 10;
	$scope.order = 'timestamp';
	$scope.reverse = false;
	$scope.total = 0;
	$scope.items = [];

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
			id : $scope.id,
			type : 'list',
			offset : $scope.offset, 
			limit : $scope.limit,
			order : $scope.order,
			reverse : $scope.reverse
		};
	};
	$scope.refresh = function(params) {
		$scope.search([ $.extend($scope.params(), params) ], function(result) {
			$.extend($scope, params);
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		$scope.total = result.total;
		$scope.items = result[$scope.id];
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

function EventListConfigCtrl() {
	var $scope = this;
	$scope.limit = $scope.$parent.limit;
	$scope.save = function() {
		$scope.refresh({ offset : 0, limit : $scope.limit });
		$('#event-list-config-dialog').modal('hide');
	};
}

function TagCountCtrl() {

	var $scope = this;
	$scope.id = 'tags';
	$scope.field = 'tag';
	$scope.order = 'count';
	$scope.reverse = false;
	$scope.offset = 0;
	$scope.limit = 10;
	$scope.more = false;
	$scope.tags = [];

	$scope.hasPrev = function() {
		return $scope.offset > 0;
	}
	$scope.hasNext = function() {
		return $scope.more;
	}
	$scope.prev = function() {
		$scope.refresh({ offset : $scope.offset - $scope.limit });
	}
	$scope.next = function() {
		$scope.refresh({ offset : $scope.offset + $scope.limit });
	}
	$scope.setOrder = function(order) {
		$scope.refresh({ offset : 0, order : order, reverse : order == $scope.order && !$scope.reverse });
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
			id : $scope.id,
			type : 'count',
			field : $scope.field, 
			offset : $scope.offset, 
			limit : $scope.limit,
			order : $scope.order,
			reverse : $scope.reverse
		};
	};
	$scope.refresh = function(params) {
		$scope.search([ $.extend($scope.params(), params) ], function(result) {
			$.extend($scope, params)
			$scope.update(null, result);
		});
	};
	$scope.update = function(event, result) {
		var tags = result[$scope.id];
		$scope.more = tags.length > $scope.limit;
		$scope.tags = tags.slice(0, $scope.limit);
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

function TagCountConfigCtrl() {
	var $scope = this;
	$scope.limit = $scope.$parent.limit;
	$scope.save = function() {
		$scope.refresh({ offset : 0, limit : $scope.limit });
		$('#tag-count-config-dialog').modal('hide');
	};
}

function TagGanttCtrl() {

	var $scope = this;
	$scope.id = 'tagsGantt';
	$scope.tokenField = 'tag';
	$scope.timeField = 'timestamp';
	$scope.order = 'max';
	$scope.limit = 10;
	$scope.tags = [];

	$scope.params = function() {
		return { 
			id : $scope.id,
			type : 'gantt',
			tokenField : $scope.tokenField, 
			timeField : $scope.timeField,
			timezone : locale.timezoneOffset,
			order : $scope.order,
			limit : $scope.limit
		};
	};
	$scope.update = function(event, result) {
		$scope.tags = result[$scope.id];
		$.each($scope.tags, function(i, tag) {
			tag.freq = Math.round((new Date(tag.last).getTime() - new Date(tag.first).getTime()) / tag.count);
		});
	};
	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

function RatingCountCtrl() {

	var $scope = this;
	$scope.id = 'ratings';
	$scope.field = 'rating';
	$scope.from = 10;
	$scope.to = 90;
	$scope.step = 20;
	$scope.ratings = [];

	$scope.params = function() {
		return { 
			id : $scope.id,
			type : 'histogram',
			field : $scope.field, 
			from : $scope.from,
			to : $scope.to,
			step : $scope.step
		};
	};
	$scope.update = function(event, result) {
		$scope.ratings = result[$scope.id];
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

function ScoreboardCtrl() {

	var $scope = this;
	$scope.id = 'distances';
	$scope.title = 'Scoreboard';
	$scope.tokenField = 'creator';
	$scope.valueField = 'distance';
	$scope.order = 'total';
	$scope.limit = 10;
	$scope.users = [];

	$scope.params = function() {
		return { 
			id : $scope.id,
			type : 'scoreboard',
			tokenField : $scope.tokenField, 
			valueField : $scope.valueField,
			order : $scope.order,
			limit : $scope.limit
		};
	};
	$scope.update = function(event, result) {
		$scope.users = result[$scope.id];
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

function ScoreboardConfigCtrl() {
	var $scope = this;
	$scope.title = $scope.$parent.title;
	$scope.limit = $scope.$parent.limit;
	$scope.valueField = $scope.$parent.valueField;
	$scope.save = function() {
		$scope.$parent.title = $scope.title;
		$scope.$parent.limit = $scope.limit;
		$scope.$parent.valueField = $scope.valueField;
		$scope.refresh();
		$('#scoreboard-config-dialog').modal('hide');
	};
}

function TimelineCtrl() {

	var $scope = this;
	$scope.id = 'timeline';
	$scope.field = 'timestamp';
	$scope.intervals = [ 'year', 'month', 'day', 'hour', 'minute' ];
	$scope.intervalLengths = [ 4, 7, 10, 13, 16 ];
	$scope.interval = 1;
	$scope.range = '';
	$.each($scope.getFilters($scope.field), function(i, filter) {
		$scope.range = filter.value;
		$.each($scope.intervalLengths, function(j, length) {
			if ($scope.range.length == length) {
				$scope.interval = Math.min(j + 1, $scope.intervals.length);
			}
		});
	});
	$scope.times = [];

	$scope.currentInterval = function() {
		return $scope.intervals[$scope.interval];
	};
	$scope.zoomIn = function() {
		$scope.interval = Math.min($scope.interval + 1, $scope.intervals.length - 1);
	};
	$scope.params = function() {
		return { 
			id : $scope.id,
			type : 'timeline',
			field : $scope.field, 
			interval : $scope.currentInterval(),
			range : $scope.range,
			timezone : locale.timezoneOffset
		};
	};
	$scope.update = function(event, result) {
		$scope.times = result[$scope.id];
		$scope.draw();
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

TimelineCtrl.prototype.draw = function() {
	var $scope = this;
	if ($scope.times.length) {
		google.load("visualization", "1", { packages : [ "corechart" ], callback : function() { 
			var data = new google.visualization.DataTable();
			data.addColumn('string', $scope.currentInterval());
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
				hAxis : { baselineColor : 'white', textPosition : 'none' }, 
			};
			var chart = new google.visualization.ColumnChart(document.getElementById('timeline'));
			chart.draw(data, options);
			google.visualization.events.addListener(chart, 'select', function() {
				var selection = chart.getSelection();
				var value = data.getValue(selection[0].row, 0);
				$scope.zoomIn();
				$scope.addFilter(new Filter($scope.field, value), true);
				$scope.refresh();
			});
		}});
	} else {
		$('#timeline').html('<i class="none">None</i>');
	}
}

function MapCtrl() {

	var $scope = this;
	$scope.id = 'map';
	$scope.field = 'location';
	$scope.map = null;

	$scope.update = function(event, result) {
		var points = [ ];
		$scope.events = $.each(result['events'], function(i, event) {
			var location = event[$scope.field];
			if (location) {
				points.push(location);
			}
		});
		$scope.draw(points);
	};
	$scope.filterBounds = function() {
		$scope.addFilter(new Filter($scope.field, $scope.map.getBounds().toUrlValue(2)), true);
	};

	$scope.register($scope);
	$scope.$on('result', $scope.update);
}

MapCtrl.prototype.draw = function(points) {
	var $scope = this;
	if (points.length) {
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
			$.each(points, function(i, point) {
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

MapCtrl.prototype.createFilterControl = function() {
	var $scope = this;
	var parent = document.createElement('div');
	parent.style.padding = '5px';
	var control = document.createElement('div');
	control.title = 'Click to filter using the current map bounds';
	control.className = 'control';
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

TemplateCtrl.$inject = ['$http', '$defer', '$routeParams'];
function TemplateCtrl($http, $defer, $routeParams) {
	var $scope = this;
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
			distance : 10000.0,
			height : 550.0
		},
		{
			random : 1000
		}
	];
	$scope.create = function() {
		$http.post('/buckets/' + $scope.params.bucketId + '/', $scope.content).success(function(response, status, headers) {
			var location = headers('Location');
			var undo = headers('Undo');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			console.assert(undo, 'missing undo header');
			$defer(function() {
				$scope.reload();
				$scope.alert.show('Created a new event.', 'alert-success', undo);
				$('#create-event-dialog').modal('hide');
			}, 1000);
		});
	}
	$scope.getTemplate = function(i) {
		return JSON.stringify($scope.templates[i], null, ' ');
	}
}

EventCtrl.$inject = ['$http', '$routeParams'];
function EventCtrl($http, $routeParams) {
	var $scope = this;
	$scope.params = $routeParams;
	$http.get('/buckets/' + $scope.params.bucketId + '/' + $scope.params.eventId).success(function(response, code) {
		$scope.event = response;
	});
}

var fields = [
	{
		name : 'tag', 
		format : function(value) { 
			return '<span class="nowrap" title="Tag">' +
				        '<i class="icon-tag"></i> ' + encode(value) +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'resource',
		format : function(value) {
			return '<span title="Resource">' +
				        '<i class="icon-bookmark"></i>&nbsp;' +
				        '<a href="' +  encode(value.url) + '">' +  encode(value.title) + '</a>' +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'distance', 
		format : function(value) { 
			return '<span class="nowrap" title="Distance">' +
				        '<i class="icon-resize-horizontal"></i> ' + encode(value) + 'm' +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'height', 
		format : function(value) { 
			return '<span class="nowrap" title="Height">' +
				        '<i class="icon-resize-vertical"></i>' + encode(value) + 'm' +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'location',
		format : function(value) {
			return '<span class="nowrap" title="Location">' +
				        '<i class="icon-map-marker"></i> ' +
				        '<a href="http://maps.google.com/maps?q=' + 
				        encode(value.lat + ',' + value.lon) + '&t=p&z=5">' + 
				        encode(value.lat + ', ' + value.lon) + '</a>' +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'timestamp',
		format : function(value) {
			return '<span class="nowrap">' +
				         '<i class="icon-calendar" title="Timestamp"></i><abbr title="' + value + '"> ' + humane.date(new Date(Date.parse(value))) +
				       '</abbr></span> &nbsp; ';
		}
	},
	{
		name : 'duration',
		format : function(value) {
			return '<span class="nowrap">' +
				         '<i class="icon-time" title="Duration"></i> ' + humane.duration(value, false) +
				       '</span> &nbsp; ';
		}
	},
	{
		name : 'rating',
		format : function(value) {
			var stars = Math.round((value || 0) / 20);
			var html = '<span class="nowrap" title="Rated ' + stars + '/5">';
			for (var i = 0; i < 5; ++i) {
				html += '<i class="' + (stars > i ? 'icon-star' : 'icon-star-empty') + '"></i>';
			}
			html += '</span> &nbsp; ';
			return html;
		}
	},
	/*{
		name : 'creator',
		format : function(value) {
			return '<span class="nowrap">' +
				         '<i class="icon-user" title="User"></i> ' + value +
				       '</span> &nbsp; ';
		}
	}*/
];

function getField(name) {
	for (var i = 0; i < fields.length; ++i) {
		if (fields[i].name === name) {
			return fields[i];
		}
	}
	return;
}

/*angular.widget('zeno:event', function(compileElement) {
	return function(linkElement) {
		var $scope = this;
		function update() {
			var event = $scope.$eval(linkElement.attr('value'));
			var html = '';
			$.each(fields, function(i, field) {
				var value = event[field.name];
				if (value) {
					if ($.isArray(value)) {
						$.each(value, function(i, value) {
							html += field.format(value);
						});
					}
					else {
							html += field.format(value);
					}
				}
			});
			linkElement.append(html);
		}
		update();
	};
});*/

var app = angular.module('ZenoModule', []);

app.filter('fields', function() {
	return function(event) {
		var html = '';
		$.each(fields, function(i, field) {
			var value = event[field.name];
			if (value) {
				if ($.isArray(value)) {
					$.each(value, function(i, value) {
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
	var field = getField('rating');
	return function(rating) {
		return field.format(rating);
	}
});

app.config(function($httpProvider) {
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
});

angular.widget('zeno:copyright', function(compileElement) {
	var start = compileElement.attr('start');
	var author = compileElement.attr('author');
	return function(linkElement) {
		var year = new Date().getFullYear();
		var text = start == year ?
			'&copy; ' + start + ' ' + author :
			'&copy; ' + start + '&ndash;' + year + ' ' + author;
		linkElement.html(text);
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

var locale = {
		timezoneOffset : -new Date().getTimezoneOffset()
};
