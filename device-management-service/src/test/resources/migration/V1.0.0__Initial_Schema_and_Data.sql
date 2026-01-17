CREATE TABLE devices (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- e.g., TEMP_SENSOR
    status VARCHAR(20) NOT NULL, -- e.g., ACTIVE
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION,

    CONSTRAINT chk_coordinates_pair
            CHECK (
                (longitude IS NULL AND latitude IS NULL) OR
                (longitude IS NOT NULL AND latitude IS NOT NULL)
            )
);

-- --------------------------------------------------------
-- RULES TABLE (Many side of the One-to-Many relationship)
-- --------------------------------------------------------
CREATE TABLE rules (
    id UUID PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    from_value INTEGER NOT NULL,
    to_value INTEGER NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    -- Foreign Key to link the rule to its parent device
    device_id UUID NOT NULL,
    CONSTRAINT fk_rule_device
        FOREIGN KEY (device_id)
        REFERENCES devices (id)
        ON DELETE CASCADE -- If device is deleted, delete its rules too
);


INSERT INTO devices (id, name, type, status, longitude, latitude, description, created_at, updated_at) VALUES
(
    '1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d', -- Device ID for Thermostat (Matches Rule 1)
    'Living Room Thermostat',
    'TEMP_SENSOR',
    'ACTIVE',
    50.0755, -- Example Longitude (e.g., Prague)
    14.4378, -- Example Latitude (e.g., Prague)
    'Main thermostat for the house.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'b2c3d4e5-f6e5-4d3c-2b1a-0fedcba98765', -- Device ID for Smart Light (Matches Rule 2)
    'Hallway Smart Light',
    'LIGHT',
    'ACTIVE',
    50.0753, -- Example Longitude (Slightly different from above)
    14.4380, -- Example Latitude
    'Smart light with dimming capabilities.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'c3d4e5f6-a1b2-3c4d-5e6f-789012345678', -- Device ID for Motion Sensor (Matches Rule 3)
    'Backyard Motion Sensor',
    'MOTION_SENSOR',
    'ACTIVE',
    NULL, -- Example Longitude (Further afield)
    NULL, -- Example Latitude
    'Sensor to detect movement for security alerts.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    -- Device 7: Solar Panel Power Meter in Niger
    'a7b8c9d0-e1f2-3a4b-5c6d-7e8f01234567',
    'Niger Solar Meter',
    'ENERGY_METER',
    'ACTIVE',
    8.0817,   -- Longitude for Niger (Niamey)
    17.6078,  -- Latitude for Niger (Niamey)
    'Measures power output from solar array.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);



INSERT INTO rules (id, rule_name, from_value, to_value, description, created_at, updated_at, device_id) VALUES
(
    -- Rule 1: Temperature safety range for thermostat
        '89d5c071-79aa-4def-9272-1b3b1aecbb1e', -- Rule ID
        'temperature',
        18,
        22,
        'Low Temp Auto-Adjust: If the temperature falls below 18 degrees, set the target temperature to 22 degrees.',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        '1a1b1c1d-2e2f-3a3b-4c4d-5e5f5a5b5c5d' -- <-- MUST MATCH devices.-
),
(
    -- Rule 2: Brightness safety range for smart light
    'dfaa62df-1b0c-4ed7-8421-1fc7cf45879f', -- Used a valid placeholder UUID
    'brightness',
    30,
    100,
    'Nighttime Dimming: After 10 PM, if the light is on, dim its brightness to 30% for night vision.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'b2c3d4e5-f6e5-4d3c-2b1a-0fedcba98765' -- Smart Light's ID (VALID)
),
(
    -- Rule 3: Motion detection safety range - Range 0.5 to 2.5
    '2a8f897f-8f07-4a1f-8bc8-70ad3b08edaa', -- Motion Sensor Rule ID (Unchanged)
    'motion',
    1, -- Motion intensity/score starts at 1
    5, -- Motion intensity/score ends at 5
    'Backyard Motion Alert: Trigger a security alert if the motion score/intensity is between 0.5 and 2.5.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'c3d4e5f6-a1b2-3c4d-5e6f-789012345678' -- Motion Sensor's ID (Unchanged)
),
(
    -- Rule 7: Minimum Power Output (Niger)
    '7d8e9f0a-1b2c-3d4e-5f6a-7b8c9d0e1f20',
    'low_power_output',
    500, -- Minimum acceptable power output in Watts
    99999,
    'Panel Health Check: If power output drops below 500W during peak hours, send a maintenance notification.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'a7b8c9d0-e1f2-3a4b-5c6d-7e8f01234567' -- Niger Solar Meter's ID
);



COMMIT;