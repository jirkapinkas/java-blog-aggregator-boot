
JBA.news = {};

/*
 * administrator functionality
 */

JBA.news.remove = function (e) {
	var origin = $(e);
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
            	var newsId = origin.attr("id");
			$.post("/admin/news/delete/" + newsId, function (data) {
				location.reload(true); // reload page
			});
            	dialog.close();
            }
        }]
    });
}
