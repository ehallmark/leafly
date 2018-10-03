


$(document).ready(function() {
    $('form.strain_recommendation').submit(function(e) {
        e.preventDefault();
        var data = $(this).serialize();
        $.ajax({
            url: '/recommend',
            data: data,
            dataType: 'json',
            type: 'POST',
            success: function(result) {
                $('#results').html(result.data);
            }
        });
    });

    $('select.strain_selection').select2({
        minimumResultsForSearch: 10,
        closeOnSelect: true,
        placeholder: 'Select multiple strains'
    });
});