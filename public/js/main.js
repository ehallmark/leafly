


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
        placeholder: 'Select multiple strains...'
    });
    $('select.product_selection').select2({
        minimumResultsForSearch: 10,
        closeOnSelect: true,
        placeholder: 'Select multiple products...',
        ajax: {
            url: '/products_ajax',
            dataType: 'json'
            // Additional AJAX parameters go here; see the end of this chapter for the full code of this example
        }
    });
});