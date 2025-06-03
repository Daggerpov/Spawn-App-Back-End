# **Setting Up the `spawn_db` Database Locally**

Follow these steps to download, set up the database locally, create `spawn_db`, and populate it with sample data.

---

## **1. Download and Install MySQL**

1. **Download MySQL Community Server**:
    - Visit the [MySQL Downloads Page](https://dev.mysql.com/downloads/mysql/).
    - Choose your operating system and download the installer.
    - Follow the installation wizard steps.
    - Configure the root password during setup and remember it for later use.

2. **(Optional) Download MySQL Workbench**:
    - Visit the [MySQL Workbench Page](https://dev.mysql.com/downloads/workbench/).
    - Install it to have a graphical interface to work with your MySQL server.

---

## **2. Set Up the Database**

1. **Open MySQL Workbench or Terminal**:
    - If using MySQL Workbench, connect to your MySQL server and log in.
    - If using Terminal, log in to the MySQL server by running:
      ```bash
      mysql -u root -p
      ```
    - Enter your root password when prompted.

2. **Create the `spawn_db` Database**:
    - Run the following command to create the database:
      ```sql
      CREATE DATABASE spawn_db;
      ```

3. **Use the `spawn_db` Database**:
    - Run the following command:
      ```sql
      USE spawn_db;
      ```

4. **Ensure Environment Variables are Set**
   - Set the following environment variables, or add to a `.env` file
     - `MYSQL_URL`
     - `MYSQL_USER`
     - `MYSQL_PASSWORD`

5. **Populate the Database with Sample Data**:
    - If you are using the terminal, create a file named `populate_spawn_db.sql` and add the following SQL commands to it:
      ```sql
      -- Use the spawn_db database
      USE spawn_db;
      
      -- Populate Users
      INSERT INTO user (id, username, first_name, last_name, bio, profile_picture) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), 'john_doe', 'John', 'Doe', 'Loves hiking and coffee.', 'profile1.png'),
      (UNHEX(REPLACE(UUID(), '-', '')), 'jane_smith', 'Jane', 'Smith', 'Digital nomad and bookworm.', 'profile2.png'),
      (UNHEX(REPLACE(UUID(), '-', '')), 'sam_wilson', 'Sam', 'Wilson', 'Coder by day, gamer by night.', 'profile3.png'),
      (UNHEX(REPLACE(UUID(), '-', '')), 'alex_jones', 'Alex', 'Jones', 'Photographer with a passion for travel.', 'profile4.png');
      
      -- Populate Locations
      INSERT INTO location (id, name, latitude, longitude) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), 'Central Park', 40.785091, -73.968285),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Times Square', 40.758896, -73.985130),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Golden Gate Park', 37.769042, -122.483519),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Eiffel Tower', 48.858844, 2.294351);
      
      -- Populate Activities
      INSERT INTO Activity (id, title, start_time, end_time, location_id, note, creator_id) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), 'Hiking Adventure', '2024-12-01T08:00:00', '2024-12-01T16:00:00',
      (SELECT id FROM location WHERE name='Central Park'), 'Bring snacks and water.',
      (SELECT id FROM user WHERE username='john_doe')),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Book Club Meeting', '2024-12-05T18:00:00', '2024-12-05T20:00:00',
      (SELECT id FROM location WHERE name='Times Square'), 'Discussing the latest thriller.',
      (SELECT id FROM user WHERE username='jane_smith')),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Photography Workshop', '2024-12-10T10:00:00', '2024-12-10T15:00:00',
      (SELECT id FROM location WHERE name='Golden Gate Park'), 'Learn the basics of DSLR photography.',
      (SELECT id FROM user WHERE username='alex_jones'));
      
      -- Populate Activity Participants
      INSERT INTO Activity_participants (Activity_id, user_id) VALUES
      ((SELECT id FROM Activity WHERE title='Hiking Adventure'), (SELECT id FROM user WHERE username='jane_smith')),
      ((SELECT id FROM Activity WHERE title='Hiking Adventure'), (SELECT id FROM user WHERE username='sam_wilson')),
      ((SELECT id FROM Activity WHERE title='Book Club Meeting'), (SELECT id FROM user WHERE username='john_doe')),
      ((SELECT id FROM Activity WHERE title='Book Club Meeting'), (SELECT id FROM user WHERE username='alex_jones'));
      
      -- Populate Activity Invited
      INSERT INTO Activity_invited (Activity_id, user_id) VALUES
      ((SELECT id FROM Activity WHERE title='Photography Workshop'), (SELECT id FROM user WHERE username='john_doe')),
      ((SELECT id FROM Activity WHERE title='Photography Workshop'), (SELECT id FROM user WHERE username='jane_smith'));
      
      -- Populate Friend Tags
      INSERT INTO friend_tag (id, display_name, color) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), 'Close Friends', '#FF5733'),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Work Friends', '#33FF57'),
      (UNHEX(REPLACE(UUID(), '-', '')), 'Family', '#3357FF');
      
      -- Populate Friend Requests
      INSERT INTO friend_requests (id, sender_id, receiver_id) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='john_doe'), (SELECT id FROM user WHERE username='jane_smith')),
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='sam_wilson'), (SELECT id FROM user WHERE username='alex_jones'));
      
      -- Populate User Friends
      INSERT INTO user_friends (id, friend_1, friend_2) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='john_doe'), (SELECT id FROM user WHERE username='sam_wilson')),
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='jane_smith'), (SELECT id FROM user WHERE username='alex_jones'));
      
      -- Populate User Friend Tags
      INSERT INTO user_friend_tags (id, user_id, friend_tag_id) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='john_doe'), (SELECT id FROM friend_tag WHERE display_name='Close Friends')),
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='jane_smith'), (SELECT id FROM friend_tag WHERE display_name='Work Friends'));
      
      -- Populate User Friend Tag Mapping
      INSERT INTO user_friend_tag_mapping (id, user_1, user_2, friend_tag_id) VALUES
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='john_doe'), (SELECT id FROM user WHERE username='sam_wilson'), (SELECT id FROM friend_tag WHERE display_name='Close Friends')),
      (UNHEX(REPLACE(UUID(), '-', '')), (SELECT id FROM user WHERE username='jane_smith'), (SELECT id FROM user WHERE username='alex_jones'), (SELECT id FROM friend_tag WHERE display_name='Work Friends'));
      ```

6. **Run the Script**:
    - In the terminal, run:
      ```bash
      mysql -u root -p spawn_db < populate_spawn_db.sql
      ```
    - If using MySQL Workbench, paste the script into the query window and run it.

Thats all! You have successfully set up the `spawn_db` database locally and populated it with sample data. You can now use it to test the Spawn application. (hopefully)
   
