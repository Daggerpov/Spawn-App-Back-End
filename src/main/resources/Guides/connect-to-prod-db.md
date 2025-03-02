# Connecting to Prod (Hosted) DB Through IntelliJ


1. Create new data source from URL

![img_1.png](images_connect-to-prod-db/img_1.png)

2. On Railway -> go to our database's container -> "Data" tab -> click "Connect"

![img_3.png](images_connect-to-prod-db/img_3.png)

3. Click "Public Network" -> copy the first URL

![img_4.png](images_connect-to-prod-db/img_4.png)

4. Paste that into the IntelliJ connection window

![img_5.png](images_connect-to-prod-db/img_5.png)

5. Click "no auth" instead of user/pass login

![img_6.png](images_connect-to-prod-db/img_6.png)

6. Prepend `jdbc:` to the URL and click "test connection"

![img_7.png](images_connect-to-prod-db/img_7.png)

7. It should work now, and show our DB tables in this tab:

![img_8.png](images_connect-to-prod-db/img_8.png)