
JBA.account = {};

/*
 * jquery validation of blog form
 */

JBA.account.validate = {
	rules : {
		name : {
			required : true,
			minlength : 1
		},
		url : {
			required : true,
			url : true,
			remote : {
				url : "/blog/available",
				type : "get",
				data : {
					url : function() {
						return $("#url")
								.val();
					}
				}
			}
		},
		homepageUrl : {
			required : true,
			minlength : 1,
			url : true
		},
		shortName : {
			required : true,
			minlength : 1,
			remote : {
				url: "/blog/shortname/available",
				type: "get",
				data: {
					username: function() {
						return $("#shortName").val();
					}
				}
			}
		}
	},
	highlight : function(element) {
		$(element).closest('.form-group').removeClass('has-success').addClass('has-error');
	},
	unhighlight : function(element) {
		$(element).closest('.form-group').removeClass('has-error').addClass('has-success');
	},
	messages : {
		url : {
			remote : "Such blog already exists!"
		},
		shortName: {
			remote: "Such short name already exists!"
		}
	}
};

JBA.account.remove = function () {
		var origin = $(this);
		BootstrapDialog
			.show({
				title : 'Really delete?',
				message : 'Really delete?',
				buttons : [
					{
						label : 'Cancel',
						action : function(dialog) {
							dialog.close();
						}
					},
					{
						label : 'Delete',
						cssClass : 'btn-primary',
						action : function(dialog) {
							var blogId = origin.attr("id");
							$.post("../blog/remove/" + blogId,
								function(data) {
									location.reload(true); // reload page
								});
							dialog.close();
						}
					} ]
	});
}
