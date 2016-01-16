
JBA.adminCategories = {};

JBA.adminCategories.add = function(e) {
	$("#name").val("");
	$("#shortName").val("");
	$("#id").val("0");
	$('#myModal').modal();
}

JBA.adminCategories.edit = function(e) {
	$.get("/admin/categories/" + $(this).attr("id"), function (data) {
		$("#name").val(data.name);
		$("#shortName").val(data.shortName);
		$("#id").val(data.id);
	});
	$('#myModal').modal();
}

JBA.adminCategories.remove = function (e) {
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
            	var categoryId = origin.attr("id");
				$.post("/admin/categories/delete/" + categoryId, function (data) {
					location.reload(true); // reload page
				});
            	dialog.close();
            }
        }]
    });
}
