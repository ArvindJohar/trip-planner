ALTER TABLE destinations
ADD COLUMN sort_order INTEGER;

ALTER TABLE destinations
ADD COLUMN days INTEGER;

-- Backfill existing destinations so the migration works
-- with your current database data.
WITH ordered_destinations AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY trip_id
            ORDER BY start_date, id
        ) AS destination_order,
        (end_date - start_date + 1) AS destination_days
    FROM destinations
)
UPDATE destinations d
SET
    sort_order = o.destination_order,
    days = o.destination_days
FROM ordered_destinations o
WHERE d.id = o.id;

ALTER TABLE destinations
ALTER COLUMN sort_order SET NOT NULL;

ALTER TABLE destinations
ALTER COLUMN days SET NOT NULL;

ALTER TABLE destinations
ADD CONSTRAINT chk_destination_days_positive
CHECK (days > 0);

ALTER TABLE destinations
ADD CONSTRAINT chk_destination_sort_order_positive
CHECK (sort_order > 0);