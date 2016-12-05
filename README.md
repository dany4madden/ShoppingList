# ShoppingList

Description:
An Android app to create and manage grocery shopping lists with touch and voice commands. This app uses https://api.ai/ to process user's speech to determine user's intent.

Authors: Dany Madden and Gustaf Hegnell

Supported listName are: 
	- http://web.cecs.pdx.edu/~dany/SpokenLang/domain/listName

Supported itemNames are: 
	- http://web.cecs.pdx.edu/~dany/SpokenLang/domain/itemName 
	- various types of itemName: http://web.cecs.pdx.edu/~dany/SpokenLang/domain/

Usage:
Main Activity page, labeled ListBuddy, contains all created lists. While on this page you can create, delete, open or count lists.
Supported voice commands are:
- "Create a Safeway list"
- "Add apples to Safeway list" (Equivalent to saying "Create Safeway". Then, "open Safeway". Then, "add apples.")
- "Delete a Safeway list"
- "Open Costco"
- "Count lists"

To get to the Managed list page, labeled with the list name, say "Open listName". Or simpily click on the list name. 
To delete a list or an item on the list with touch, touch and hold on to the list name/item for two seconds to activate the delete dialog. 

The supported voice command on the managed list page are:
- "Add milk" or "Add milk to list" or "milk"
- "Put apple on the list" or "apple"

- "Delete milk"
- "Remove chicken"

- "Count items"
- "Go back to Main"

The app also understand phrases like:
- Exit
- Help
- Thank you.
- How old are you?
- I'm bored.

Try a few phrases and see how it resonds! Have fun.

