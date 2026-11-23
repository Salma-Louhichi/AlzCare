-- Initial Schema for Identity Service
-- Creates tables for patient profiles, doctor profiles, caregiver profiles,
-- and autonomy assessments.

-- Gender Enum Type
CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE');

-- Language Enum Type
CREATE TYPE language_enum AS ENUM ('ARABIC', 'FRENCH', 'ENGLISH');

-- Autonomy Level Enum Type
CREATE TYPE autonomy_level AS ENUM ('INDEPENDENT', 'ASSISTED', 'DEPENDENT');

-- Patient Profile Table
CREATE TABLE patient_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender gender_enum NOT NULL,
    preferred_language language_enum DEFAULT 'ENGLISH',
    photo_url VARCHAR(500),
    phone VARCHAR(100),
    emergency_contact VARCHAR(255),
    address TEXT,
    version BIGINT DEFAULT 0,
    created_by UUID,
    total_points INTEGER DEFAULT 0,
    current_streak INTEGER DEFAULT 0,
    last_played_date DATE,
    assisted_mode_active BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Doctor Profile Table
CREATE TABLE doctor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    speciality VARCHAR(200),
    license_number VARCHAR(100) UNIQUE,
    photo_url VARCHAR(500),
    phone VARCHAR(100),
    contact VARCHAR(255),
    address TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Caregiver Profile Table
CREATE TABLE caregiver_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    photo_url VARCHAR(500),
    phone VARCHAR(100),
    address TEXT,
    contact VARCHAR(255),
    is_professional BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Autonomy Assessment Table
CREATE TABLE autonomy_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    hygiene_level autonomy_level NOT NULL,
    medication_level autonomy_level NOT NULL,
    mobility_level autonomy_level NOT NULL,
    feeding_level autonomy_level NOT NULL,
    overall_score INTEGER CHECK (overall_score >= 1 AND overall_score <= 10),
    assessed_by UUID NOT NULL,
    notes TEXT,
    assessment_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_patient_user_id ON patient_profiles(user_id);
CREATE INDEX idx_patient_active ON patient_profiles(is_active);
CREATE INDEX idx_doctor_user_id ON doctor_profiles(user_id);
CREATE INDEX idx_caregiver_user_id ON caregiver_profiles(user_id);
CREATE INDEX idx_autonomy_patient_id ON autonomy_assessments(patient_id);
CREATE INDEX idx_autonomy_assessment_date ON autonomy_assessments(assessment_date DESC);

-- Comments
COMMENT ON TABLE patient_profiles IS 'Core patient demographic and preference data with gamification fields';
COMMENT ON TABLE doctor_profiles IS 'Doctor/clinician profile information';
COMMENT ON TABLE caregiver_profiles IS 'Caregiver profile and availability';
COMMENT ON TABLE autonomy_assessments IS 'Patient autonomy level assessments across different domains';
