function getUser(token) {
	var tokens = token ?
		token.split('-') : [];
	return tokens.length > 1 ?
		new User(tokens[1]) : undefined;
}

MainCtrl.$inject = ['$route', '$http', '$location', '$cookies'];
function MainCtrl($route, $http, $location, $cookies) {
	var self = this;
	self.user = getUser($cookies.token);
	self.alert = new Alert();
	self.undo = function(commandId) {
		$http({ method : 'POST', url : commandId, data : 'undo', headers : { 'Content-Type' : 'application/x-www-form-urlencoded' }}).success(function(response, code) {
			self.alert.clear();
			$location.url('/');
			self.reload();
		});
	};
	$route.when('/', { template: '/public/dashboard.html' });
	$route.when('/buckets/:bucketId/', { template: '/public/bucket.html' });
	$route.when('/buckets/:bucketId/:eventId', { template: '/public/event.html' });
	$route.when('/terms', { template: '/public/terms.html' });
	$route.when('/privacy', { template: '/public/privacy.html' });
	$route.otherwise({ redirectTo: '/' });
	$route.parent(this);
	self.reload = function() {
		$route.reload();
	};
	self.signOut = function() {
		$http({ method : 'POST', url : '/signout', headers : { 'Content-Type' : 'application/x-www-form-urlencoded' }}).success(function(response, code) {
				self.user = null;
				self.alert.show('Signed out.', 'alert-success');
				if ($location.url() == '/') {
					self.reload();
				} else {
					$location.url('/');
				}
		});
	};
	self.setUser = function(user) {
		self.user = user;
	};
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

AuthFormCtrl.$inject = ['$http', '$location'];
function AuthFormCtrl($http, $location) {
	var self = this;
	self.username = '';
	self.password = '';
	self.remember = false;
	self.signIn = function() {
		$http({ method : 'POST', url : '/signin', data : $.param({ username : self.username, password : self.password, remember : self.remember }), headers : { 'Content-Type' : 'application/x-www-form-urlencoded' }}).success(function(response, code) {
			self.setUser(new User(self.username));
			self.username = '';
			self.password = '';
			self.alert.clear();
			$('#sign-in-dialog').modal('hide');
			self.reload();
		});
	}
}

HistoryCtrl.$inject = ['$http'];
function HistoryCtrl($http) {
	var self = this;
	self.offset = 0;
	self.limit = 10;
	self.refresh = function(offset) {
		$http.get('/queue/?offset=' + offset + "&limit=" + self.limit).success(function(response, code) {
			self.history = response;
			self.offset = offset;
		});
	};
	self.hasPrev = function() {
		return self.offset > 0;
	}
	self.hasNext = function() {
		return self.history && self.offset + self.limit < self.history.total;
	}
	self.prev = function() {
		self.refresh(self.offset - self.limit, self.limit);
	}
	self.next = function() {
		self.refresh(self.offset + self.limit, self.limit);
	}
	self.refresh(0);
}

BucketListCtrl.$inject = ['$http'];
function BucketListCtrl($http) {
	var self = this;
	self.buckets = [ ];
	$http.get('/buckets/').success(function(response, code) {
		self.buckets = response;
	});
	self.remove = function(bucketId) {
		$http.delete('/buckets/' + bucketId + '/').success(function(response, code) {
			self.reload();
		});
	};
}

CreateBucketDialogCtrl.$inject = ['$http', '$location'];
function CreateBucketDialogCtrl($http, $location) {
	var self = this;
	self.label = 'My Data';
	self.create = function() {
		$http.post('/buckets/', { label : self.label}).success(function(data, status, headers) {
			var location = headers('Location');
			var undo = headers('Undo');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			console.assert(undo, 'missing undo header');
			self.alert.show('Created a new bucket.', 'alert-success', undo);
			$('#create-bucket-dialog').modal('hide');
			$location.url(location);
		});
	}
}

BucketCtrl.$inject = ['$http', '$routeParams', '$location'];
function BucketCtrl($http, $routeParams, $location) {
	var self = this;
	self.params = $routeParams;
	var q = $location.search()['q'];
	self.filters = q ? q.split(',') : [ ];
	self.widgets = [];
	self.register = function(widget) {
		self.widgets.push(widget);
	};
	self.search = function(widgetConfigs, callback) {
		$http.get('/buckets/' + self.params.bucketId + '/?' + $.param({ 'q' : self.filters, 'w' : widgetConfigs }, true)).success(callback);
	};
	self.refresh = function() {
		var widgetConfigs = [ ];
		$.each(self.widgets, function(i, widget) {
			widgetConfigs.push(widget.prepare());
		});
		self.search(widgetConfigs, function(response) {
			self.bucket = response;
			$.each(self.widgets, function(i, widget) {
				widget.update(response);
			});
		});
	};
	self.addFilter = function(filter) {
		if (self.filters.indexOf(filter) == -1) {
			self.filters.push(filter);
			$location.search('q', self.filters.join(','));
		}
	};
	self.removeFilter = function(filter) {
		self.filters = $.grep(self.filters, function(value) {
			return value != filter;
		});
		$location.search('q', self.filters.length ? self.filters.join(',') : null);
	};
	console.log();
	self.$evalAsync(self.refresh);
}

function EventListCtrl() {
	var self = this;

	self.offset = 0;
	self.limit = 10;
	self.hasPrev = function() {
		return self.offset > 0;
	}
	self.hasNext = function() {
		return self.offset + self.limit < self.total;
	}
	self.prev = function() {
		self.refresh(self.offset - self.limit, self.limit);
	}
	self.next = function() {
		self.refresh(self.offset + self.limit, self.limit);
	}

	self.total = 0;
	self.events = [];
	self.prepare = function(offset, limit) {
		return 'list(id:events,offset:' + defined(offset, self.offset) + ',limit:' + defined(limit, self.limit) + ',sort:dateTime,asc:false)';
	};
	self.update = function(result) {
		self.total = result.total;
		self.events = result['events'];
	};
	self.refresh = function(offset, limit) {
		self.search([ self.prepare(offset, limit) ], function(result) {
			self.offset = offset;
			self.limit = limit;
			self.update(result);
		});
	};
	self.register(self);
}

function TagCountCtrl() {
	var self = this;
	self.tags = [];
	self.prepare = function() {
		return 'count(id:tags,field:tag,limit:10)';
	};
	self.update = function(result) {
		self.tags = result['tags'];
	};
	self.register(self);
}

function RatingCountCtrl() {
	var self = this;
	self.ratings = [];
	self.prepare = function() {
		return 'histogram(id:ratings,field:rating,from:0,to:100,step:20)';
	};
	self.update = function(result) {
		self.ratings = result['ratings'];
	};
	self.register(self);
}

TemplateCtrl.$inject = ['$http', '$defer', '$routeParams'];
function TemplateCtrl($http, $defer, $routeParams) {
	var self = this;
	self.content = '';
	self.params = $routeParams;
	self.i = 0;
	self.templates = [
		undefined,
		{
			tag : [ "lunch", "pizza" ],
			location : {
				latitude : 47.62,
				longitude : -122.35
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
				latitude : 60.57,
				longitude : -151.25
			},
			distance : 10000.0,
			height : 550.0
		},
		{
			random : 1000
		}
	];
	self.create = function() {
		$http.post('/buckets/' + self.params.bucketId + '/', self.content).success(function(response, status, headers) {
			var location = headers('Location');
			var undo = headers('Undo');
			console.assert(status == 201, status);
			console.assert(location, 'missing location header');
			console.assert(undo, 'missing undo header');
			$defer(function() {
				self.reload();
				self.alert.show('Created a new event.', 'alert-success', undo);
				$('#create-event-dialog').modal('hide');
			}, 1000);
		});
	}
	self.getTemplate = function(i) {
		return JSON.stringify(self.templates[i], null, ' ');
	}
}

EventCtrl.$inject = ['$http', '$routeParams'];
function EventCtrl($http, $routeParams) {
	var self = this;
	self.params = $routeParams;
	$http.get('/buckets/' + self.params.bucketId + '/' + self.params.eventId).success(function(response, code) {
		self.event = response;
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
				        encode(value.latitude + ',' + value.longitude) + '&t=p&z=5">' + 
				        encode(value.latitude + ', ' + value.longitude) + '</a>' +
				      '</span> &nbsp; ';
		}
	},
	{
		name : 'dateTime',
		format : function(value) {
			return '<span class="nowrap" title="Date &amp; Time">' +
				         '<i class="icon-time"></i> ' + encode(value) +
				       '</span> &nbsp; ';
		}
	},
	{
		name : 'rating',
		format : function(value) {
			var stars = Math.floor(value / 20);
			var html = '<span class="nowrap" title="Rated ' + stars + '/5">';
			for (var i = 0; i < 5; ++i) {
				html += '<i class="' + (stars > i ? 'icon-star' : 'icon-star-empty') + '"></i>';
			}
			html += '</span> &nbsp; ';
			return html;
		}
	}
];

/*angular.widget('zeno:event', function(compileElement) {
	return function(linkElement) {
		var self = this;
		function update() {
			var event = self.$eval(linkElement.attr('value'));
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

angular.module('ZenoModule', [])
	.filter('fields', function() {
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
