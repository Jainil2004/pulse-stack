import csv
import os
import time
import requests
from datetime import datetime

# ====== CONFIGURATION ======
CSV_FILE_PATH = "test1.csv"  # Change to actual HWiNFO log path
INGESTION_URL = "http://localhost:8081/api/systems/ingest"
OFFSET_FILE = "last_processed_offset.txt"  # File to track last processed position
SEND_DELAY = 1.0  # Delay in seconds between each metric send

# Paste Credentials here
SYSTEM_ID = "" # paste systemid here 
AUTH_TOKEN = "" # paste auth token here

# Mapping HWiNFO column names to API field names
# Fixed column names to match actual CSV headers (with encoding-safe characters)
COLUMN_MAPPING = {
    "Core VIDs (avg) [V]": "Core_VIDs_avg_V",
    "Core Clocks (avg) [MHz]": "Core_Clocks_avg_MHz",
    "Ring/LLC Clock [MHz]": "Ring_LLC_Clock_MHz",
    "Core Usage (avg) [%]": "Core_Usage_avg_percent",
    "Core Temperatures (avg) [°C]": "Core_Temperatures_avg_C",
    "Core Temperatures (avg) [�C]": "Core_Temperatures_avg_C",  # Encoding fallback
    "Core Distance to TjMAX (avg) [°C]": "Core_Distance_to_TjMAX_avg_C",
    "Core Distance to TjMAX (avg) [�C]": "Core_Distance_to_TjMAX_avg_C",  # Encoding fallback
    "CPU Package [°C]": "CPU_Package_C",
    "CPU Package [�C]": "CPU_Package_C",  # Encoding fallback
    "CPU Package Power [W]": "CPU_Package_Power_W",
    "PL1 Power Limit (Static) [W]": "PL1_Power_Limit_Static_W",
    "PL1 Power Limit (Dynamic) [W]": "PL1_Power_Limit_Dynamic_W",
    "PL2 Power Limit (Static) [W]": "PL2_Power_Limit_Static_W",
    "PL2 Power Limit (Dynamic) [W]": "PL2_Power_Limit_Dynamic_W",
    "CPU Fan [RPM]": "CPU_FAN_RPM",
    "GPU Fan [RPM]": "GPU_FAN_RPM",
    "GPU Temperature [°C]": "GPU_Temperature",
    "GPU Temperature [�C]": "GPU_Temperature",  # Encoding fallback
    "GPU Thermal Limit [°C]": "GPU_Thermal_Limit",
    "GPU Thermal Limit [�C]": "GPU_Thermal_Limit",  # Encoding fallback
    "GPU Core Voltage [V]": "GPU_Core_Voltage",
    "GPU Power [W]": "GPU_Power",
    "GPU Clock [MHz]": "GPU_Clock",
    "GPU Core Load [%]": "GPU_Core_Load",
    "GPU Memory Usage [%]": "GPU_Memory_Usage"
}

def get_current_file_length():
    """Get current number of data rows in CSV file"""
    try:
        with open(CSV_FILE_PATH, 'r', encoding='latin1') as f:
            reader = csv.DictReader(f)
            return sum(1 for _ in reader)
    except Exception:
        return 0

def load_last_offset():
    """Load the last processed offset from file"""
    try:
        if os.path.exists(OFFSET_FILE):
            with open(OFFSET_FILE, 'r') as f:
                saved_offset = int(f.read().strip())
                print(f"✓ Last processed offset from file: {saved_offset}")
                return saved_offset
    except (ValueError, IOError) as e:
        print(f"Warning: Could not load offset file: {e}")
    
    return 0

def initialize_offset():
    """Initialize offset - detect file resets and handle appropriately"""
    current_length = get_current_file_length()
    saved_offset = load_last_offset()
    
    # Check if file has been reset (current length < saved offset)
    if current_length < saved_offset:
        print(f"🔄 FILE RESET DETECTED!")
        print(f"   Current file length: {current_length}")
        print(f"   Last processed offset: {saved_offset}")
        print(f"   → File appears to have been restarted. Starting from record 1.")
        save_last_offset(0)
        return 0
    
    # If file has grown beyond our last position, start from current end
    # This ensures we only process NEW records added after script starts
    elif current_length > saved_offset:
        print(f"File has {current_length} records, last processed: {saved_offset}")
        print(f"Setting offset to {current_length} to process only NEW records")
        save_last_offset(current_length)
        return current_length
    else:
        print(f"File has {current_length} records, resuming from offset {saved_offset}")
        return saved_offset

def save_last_offset(offset):
    """Save the current offset to file"""
    try:
        with open(OFFSET_FILE, 'w') as f:
            f.write(str(offset))
    except IOError as e:
        print(f"Warning: Could not save offset: {e}")

def simulate_machine(system_id, auth_token):
    # Initialize offset to skip existing records
    last_processed_offset = initialize_offset()
    
    while True:
        try:
            if not os.path.exists(CSV_FILE_PATH):
                print(f"Waiting for file: {CSV_FILE_PATH}")
                time.sleep(1)
                continue

            current_file_length = get_current_file_length()
            
            # Check for file reset during runtime
            if current_file_length < last_processed_offset:
                print(f"🔄 FILE RESET DETECTED DURING RUNTIME!")
                print(f"   Current file length: {current_file_length}")
                print(f"   Last processed offset: {last_processed_offset}")
                print(f"   → File has been restarted. Resetting offset to 0.")
                last_processed_offset = 0
                save_last_offset(0)
                continue
            
            # Check if new records have been added
            if current_file_length <= last_processed_offset:
                print(f"No new records. File length: {current_file_length}, Last processed: {last_processed_offset}")
                time.sleep(1)
                continue
            
            new_records_count = current_file_length - last_processed_offset
            print(f"Found {new_records_count} new record(s). Processing from offset {last_processed_offset + 1}...")
            
            # Use latin1 encoding to handle special characters properly
            with open(CSV_FILE_PATH, newline='', encoding='latin1') as csvfile:
                reader = csv.DictReader(csvfile)
                
                # Skip to the offset position
                for i in range(last_processed_offset):
                    try:
                        next(reader)
                    except StopIteration:
                        break
                
                # Process new records only
                records_processed = 0
                for row_num, row in enumerate(reader, start=last_processed_offset + 1):
                    timestamp = row.get('Date/Time')

                    # Check if the timestamp was found
                    if not timestamp or not timestamp.strip():
                        print(f"Row {row_num}: Skipping row with missing 'Date/Time' field")
                        continue

                    print(f"Row {row_num}: Processing timestamp: {timestamp}")

                    metrics = {"timestamp": timestamp}
                    metrics_found = 0
                    
                    for csv_key, json_key in COLUMN_MAPPING.items():
                        value = row.get(csv_key)
                        if value and value.strip():
                            try:
                                metrics[json_key] = float(value)
                                metrics_found += 1
                            except ValueError:
                                print(f"Row {row_num}: Could not convert '{csv_key}': '{value}' to float")
                                metrics[json_key] = None
                    
                    print(f"Row {row_num}: Found {metrics_found} metrics from {len(COLUMN_MAPPING)} possible columns")

                    payload = {
                        "systemId": system_id,
                        "authToken": auth_token,
                        "metricsPayload": metrics
                    }

                    headers = {
                        "Authorization": f"Bearer {auth_token}"
                    }

                    try:
                        response = requests.post(INGESTION_URL, json=payload, headers=headers, timeout=10)
                        print(f"[{system_id}] Row {row_num}: {response.status_code} - {response.text[:100]}...")
                    except requests.exceptions.RequestException as e:
                        print(f"[{system_id}] Row {row_num}: REQUEST ERROR: {e}")

                    # Update offset after successful processing
                    last_processed_offset = row_num
                    save_last_offset(last_processed_offset)
                    records_processed += 1
                    
                    # Add delay between each metric send
                    # print(f"Waiting {SEND_DELAY} seconds before next record...")
                    time.sleep(SEND_DELAY)

            print(f"Finished processing {records_processed} new records. Waiting for more data...")
            time.sleep(1)

        except Exception as e:
            print(f"[{system_id}] ERROR: {e}")
            import traceback
            traceback.print_exc()
            time.sleep(2)

# === STARTUP TESTS ===
print("="*50)
print("PULSE STACK SIMULATION CLIENT STARTING")
print(f"System ID: {SYSTEM_ID}")
print(f"CSV File: {CSV_FILE_PATH}")
print(f"Ingestion URL: {INGESTION_URL}")
print(f"Send Delay: {SEND_DELAY} seconds between records")
print(f"Offset File: {OFFSET_FILE}")

# Test file existence
if os.path.exists(CSV_FILE_PATH):
    file_size = os.path.getsize(CSV_FILE_PATH)
    current_records = get_current_file_length()
    print(f"✓ CSV file found, size: {file_size} bytes, records: {current_records}")
else:
    print(f"✗ CSV file NOT found: {CSV_FILE_PATH}")

# Show offset status
saved_offset = load_last_offset()
current_length = get_current_file_length()
if current_length < saved_offset:
    print(f"🔄 File reset detected! Current: {current_length}, Last processed: {saved_offset}")
    print(f"   → Will start processing from record 1")
elif saved_offset == 0:
    print(f"✓ First run - will skip {current_length} existing records and process only NEW ones")
elif current_length > saved_offset:
    new_records = current_length - saved_offset
    print(f"✓ Will process {new_records} records that were added since last run")
else:
    print(f"✓ No new records to process yet (file has {current_length}, last processed: {saved_offset})")

# Test CSV headers
try:
    with open(CSV_FILE_PATH, 'r', encoding='latin1') as f:
        reader = csv.DictReader(f)
        headers = list(reader.fieldnames)
        print(f"✓ CSV headers loaded: {len(headers)} columns")
        
        # Show which columns from mapping are found
        found_cols = [col for col in COLUMN_MAPPING.keys() if col in headers]
        print(f"✓ Found {len(found_cols)} of {len(COLUMN_MAPPING)} expected columns")
        if len(found_cols) < len(COLUMN_MAPPING):
            missing = [col for col in COLUMN_MAPPING.keys() if col not in headers]
            print(f"Missing columns: {missing[:3]}...")  # Show first 3 missing
            
except Exception as e:
    print(f"✗ Error reading CSV: {e}")

print("="*50)
print()

simulate_machine(SYSTEM_ID, AUTH_TOKEN)