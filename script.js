<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vehicle Auction System</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; }
        .auction-item { border: 1px solid #ddd; margin: 20px 0; padding: 20px; border-radius: 8px; }
        .timer { font-weight: bold; color: #e74c3c; }
        .bid-input { padding: 10px; margin: 10px 0; width: 150px; }
        button { background: #3498db; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
        button:hover { background: #2980b9; }
        .current-bid { font-size: 1.2em; color: #27ae60; }
    </style>
</head>
<body>
    <h1>Vehicle Auction System</h1>
    <div id="auctions"></div>

    <script>
        // Sample vehicles data
        let auctions = [
            { id: 1, make: 'Toyota', model: 'Camry 2023', currentBid: 15000, endTime: Date.now() + 3600000, highestBidder: 'None' }, // 1 hour
            { id: 2, make: 'Honda', model: 'Civic', currentBid: 12000, endTime: Date.now() + 1800000, highestBidder: 'User1' }, // 30 min
            { id: 3, make: 'Yamaha', model: 'R1 Bike', currentBid: 8000, endTime: Date.now() + 7200000, highestBidder: 'None' }  // 2 hours
        ];

        // Load from localStorage if available
        const saved = localStorage.getItem('auctions');
        if (saved) auctions = JSON.parse(saved);

        function renderAuctions() {
            const container = document.getElementById('auctions');
            container.innerHTML = auctions.map(auction => {
                const timeLeft = Math.max(0, auction.endTime - Date.now());
                const minutes = Math.floor(timeLeft / 60000);
                const seconds = Math.floor((timeLeft % 60000) / 1000);
                const ended = timeLeft <= 0;

                return `
                    <div class="auction-item">
                        <h3>${auction.make} ${auction.model}</h3>
                        <p class="current-bid">Current Bid: $${auction.currentBid}</p>
                        <p>Highest Bidder: ${auction.highestBidder}</p>
                        <p class="timer">Time Left: ${ended ? 'Auction Ended!' : `${minutes}m ${seconds}s`}</p>
                        <input type="number" class="bid-input" id="bid-${auction.id}" placeholder="Enter bid" min="${auction.currentBid + 100}" step="100">
                        <button onclick="placeBid(${auction.id})" ${ended ? 'disabled' : ''}>Place Bid</button>
                    </div>
                `;
            }).join('');
        }

        function placeBid(id) {
            const bidInput = document.getElementById(`bid-${id}`);
            const bid = parseFloat(bidInput.value);
            const auction = auctions.find(a => a.id === id);

            if (bid > auction.currentBid) {
                auction.currentBid = bid;
                auction.highestBidder = 'You'; // Simulate current user
                alert(`Bid placed! New highest: $${bid}`);
                saveAuctions();
                renderAuctions();
            } else {
                alert('Bid must be higher than current bid!');
            }
        }

        function saveAuctions() {
            localStorage.setItem('auctions', JSON.stringify(auctions));
        }

        // Update timers and re-render every second
        setInterval(() => {
            renderAuctions();
        }, 1000);

        // Initial render
        renderAuctions();
    </script>
</body>
</html>
