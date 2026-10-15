-- 1. Allow trips to have no country
ALTER TABLE trips
    ALTER COLUMN country DROP NOT NULL;

-- 2. Add country to destinations as nullable initially
ALTER TABLE destinations
    ADD COLUMN country VARCHAR(255);

-- 3. Copy the country from each destination's associated trip
UPDATE destinations d
SET country = t.country
    FROM trips t
WHERE d.trip_id = t.id;

-- 4. Make country mandatory for destinations
ALTER TABLE destinations
    ALTER COLUMN country SET NOT NULL;

-- 5. Add sort_order as nullable initially
ALTER TABLE activities
    ADD COLUMN sort_order INTEGER;

-- 6. Assign an order to existing activities
WITH numbered_activities AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY destination_id
            ORDER BY date, start_time, id
        ) AS position
FROM activities
    )
UPDATE activities a
SET sort_order = n.position
    FROM numbered_activities n
WHERE a.id = n.id;

-- 7. Make sort_order mandatory
ALTER TABLE activities
    ALTER COLUMN sort_order SET NOT NULL;