document.getElementById('loginForm').addEventListener('submit', function(e) {
    e.preventDefault();
    var bank = document.getElementById('bank').value;
    var email = document.getElementById('email').value;
    document.getElementById('username').value = bank + ':' + email;
    this.submit();
});