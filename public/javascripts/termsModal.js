var $html = $(document.documentElement);
$('[data-remote-target]').on('click', function(e) {
    e.preventDefault();
    var $this = $(this);
    $($this.data('remote-target')).load('/modal');
});

function scrollDisable() {
    $html.css('overflow', 'hidden');
}

function scrollEnable() {
    $html.css('overflow', '');
}