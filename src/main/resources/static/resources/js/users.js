
JBA.users = {}

JBA.users.remove = function () {
	var origin = $(this);
    BootstrapDialog.show({
        title: 'Really delete?',
        message: 'Really delete?',
        buttons: [{
            label: 'Cancel',
            action: function(dialog) {
            	dialog.close();
            }
        }, {
            label: 'Delete',
            cssClass: 'btn-primary',
            action: function(dialog) {
            	var userId = origin.attr("id");
				$.post("users/remove/" + userId, function (data) {
					location.reload(true); // reload page
				});
            	dialog.close();
            }
        }]
    });
}
