CREATE TABLE place_images (
                              id BIGSERIAL PRIMARY KEY,
                              country VARCHAR(255) NOT NULL,
                              city VARCHAR(255) NOT NULL,
                              image_url VARCHAR(500) NOT NULL,
                              alt_text VARCHAR(255),
                              credit VARCHAR(255),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_place_images_place
    ON place_images (LOWER(TRIM(country)), LOWER(TRIM(city)));

INSERT INTO place_images (country, city, image_url, alt_text)
VALUES
    ('India', 'Manali',
     '/images/places/manali.webp',
     'Mountains near Manali'),

    ('India', 'Goa',
     '/images/places/goa.webp',
     'Beach in Goa');
