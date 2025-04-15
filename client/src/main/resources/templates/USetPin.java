<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Verify Card Details - MyPay</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/animate.css/4.1.1/animate.min.css"/>
<style>
body {
    font-family: 'Roboto', sans-serif;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #1e3c72 0%, #2a5298 50%, #6b48ff 100%);
}
        .container {
    background: rgba(255, 255, 255, 0.95);
    padding: 2.5rem;
    border-radius: 15px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    width: 100%;
    max-width: 450px;
}
h1 {
    text-align: center;
    font-size: 2rem;
    color: #1e3c72;
    margin-bottom: 1.5rem;
}
        .btn-primary {
    background-color: #2a5298;
    border-color: #2a5298;
}
        .btn-primary:hover {
    background-color: #1e3c72;
    border-color: #1e3c72;
}
    </style>
</head>
<body>
<div class="container animate__animated animate__fadeIn">
<h1>Verify Card Details</h1>
<p>Card Number: <span th:text="${CardData}"></span></p>
<p>UPI ID: <span th:text="${UpiId}"></span></p>
    <form th:action="@{/{bank}/verifyCard(bank=${bank})}" method="post">
        <div class="mb-3">
            <label for="number" class="form-label">Full Card Number</label>
            <input type="text" class="form-control" id="number" name="number" pattern="\d{4}-\d{4}-\d{4}-\d{4}" required>
        </div>
        <div class="mb-3">
            <label for="cvv" class="form-label">CVV</label>
            <input type="text" class="form-control" id="cvv" name="cvv" pattern="\d{3}" required>
        </div>
        <button type="submit" class="btn btn-primary w-100">Verify</button>
        <div th:if="${error}" class="alert alert-danger mt-3" th:text="${error}"></div>
    </form>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>