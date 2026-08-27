# **VENDLY REMASTERED**
the new app features a javafx frontend linked to a SQLite Database and connected through an integrated Java Backend code,
that was written in a way that keeps modules decoupled and as expandable as possible. Let's talk about features
    ## 1- Sales Window:    
        - Integrated POS with a search via name or barcode based on user preference
        - Separated pop-up window to search uncoded products in case if the user wants to see al products
        - Assign a sale to a client, and set the amount they paid, the remaining is automatically computed and added to the client's debts. If the paid amount is higher than the sale cost, the rest is used to reduce the debts of the client.
        - Set a returned product(s) or lost products
        - Save and Print button ; note that printing is not implemented
        - Queue button that opens a temp instance for other sales.
    ## 2- Products Window:
        - Lists all added products registered in the system
        - each product can be seen or edited with support for promotions where a specific number of products can be sold for a special price
        - we allow to add products, each product has: name, optional list of barcodes, cost price, retail price, stock quantity, minimum thresholds for low stock alerts, category, and an optional promotion
        - products can also be deactivated if one wants to remove them, as physical removal is not allowed
    ## 3- Clients Window:
        - Lists all clients registered
        - allows to inspect and edit debts of clients
        - each client has a name, optional company name for restaurant/shop owners..., initial debts, email, phone number, address
        - these info can be edited, but deletion is only a deactivation, no physical deletion is allowed 
    ## 4- Suppliers Window:
    ## 5- Sales History Window:
    ## 6- Purchases Window:
    ## 7- Settings Window:
    ## 8- Users Window: 