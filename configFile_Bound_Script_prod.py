import csv
import os
import time
import requests
import json
import sys
from datetime import datetime
import traceback

# ====== GLOBAL VARIABLES (will be loaded from config) ======
CONFIG = {}
COLUMN_MAPPING = {
    # This mapping remains the same as it's specific to the data format
    "Core VIDs (avg) [V]": "Core_VIDs_avg_V",
    "Core Clocks (avg) [MHz]": "Core_Clocks_avg_MHz",
    "Ring/LLC Clock [MHz]": "Ring_LLC_Clock_MHz",
    "Core Usage (avg) [%]": "Core_Usage_avg_percent",
    "Core Temperatures (avg) [°C]": "Core_Temperatures_avg_C",
    "Core Temperatures (avg) [C]": "Core_Temperatures_avg_C",  # Encoding fallback
    "Core Distance to TjMAX (avg) [°C]": "Core_Distance_to_TjMAX_avg_C",
    "Core Distance to TjMAX (avg) [C]": "Core_Distance_to_TjMAX_avg_C",  # Encoding fallback
    "CPU Package [°C]": "CPU_Package_C",
    "CPU Package [C]": "CPU_Package_C",  # Encoding fallback
    "CPU Package Power [W]": "CPU_Package_Power_W",
    "PL1 Power Limit (Static) [W]": "PL1_Power_Limit_Static_W",
    "PL1 Power Limit (Dynamic) [W]": "PL1_Power_Limit_Dynamic_W",
    "PL2 Power Limit (Static) [W]": "PL2_Power_Limit_Static_W",
    "PL2 Power Limit (Dynamic) [W]": "PL2_Power_Limit_Dynamic_W",
    "CPU Fan [RPM]": "CPU_FAN_RPM",
    "GPU Fan [RPM]": "GPU_FAN_RPM",
    "GPU Temperature [°C]": "GPU_Temperature",
    "GPU Temperature [C]": "GPU_Temperature",  # Encoding fallback
    "GPU Thermal Limit [°C]": "GPU_Thermal_Limit",
    "GPU Thermal Limit [C]": "GPU_Thermal_Limit",  # Encoding fallback
    "GPU Core Voltage [V]": "GPU_Core_Voltage",
    "GPU Power [W]": "GPU_Power",
    "GPU Clock [MHz]": "GPU_Clock",
    "GPU Core Load [%]": "GPU_Core_Load",
    "GPU Memory Usage [%]": "GPU_Memory_Usage"
}

# load the configuration file and ensure required items are present (dont temper with config file)
def load_config(filename="client-config.json"):
    """Loads configuration from a JSON file."""
    if not os.path.exists(filename):
        print(f"FATAL ERROR: Configuration file '{filename}' not found.")
        print("Please make sure the config file is in the same directory as the script.")
        sys.exit(1) # Exit the script

    try:
        with open(filename, 'r') as f:
            config_data = json.load(f)
            # Basic validation
            required_keys = ["systemId", "jwtToken", "csvFilePath", "offsetFile", "sendDelay"]
            for key in required_keys:
                if key not in config_data:
                    raise KeyError(f"Missing required key in config file: '{key}'")
            return config_data
    except (json.JSONDecodeError, KeyError, Exception) as e:
        print(f"FATAL ERROR: Could not read or parse '{filename}': {e}")
        sys.exit(1)


def get_current_file_length():
    # Get current number of data rows in CSV file
    try:
        with open(CONFIG['csvFilePath'], 'r', encoding='latin1') as f:
            reader = csv.DictReader(f)
            return sum(1 for _ in reader)
    except Exception:
        return 0

def load_last_offset():
    # Load the last processed offset from file
    try:
        if os.path.exists(CONFIG['offsetFile']):
            with open(CONFIG['offsetFile'], 'r') as f:
                saved_offset = int(f.read().strip())
                print(f"✓ Last processed offset from file: {saved_offset}")
                return saved_offset
    except (ValueError, IOError) as e:
        print(f"Warning: Could not load offset file: {e}")
    return 0

def initialize_offset():
    # Initialize offset - detect file resets and handle appropriately
    current_length = get_current_file_length()
    saved_offset = load_last_offset()
    
    if current_length < saved_offset:
        print(f"🔄 FILE RESET DETECTED!")
        print(f"   Current file length: {current_length}")
        print(f"   Last processed offset: {saved_offset}")
        print(f"   → File appears to have been restarted. Starting from record 1.")
        save_last_offset(0)
        return 0
    
    elif current_length > saved_offset:
        print(f"File has {current_length} records, last processed: {saved_offset}")
        print(f"Setting offset to {current_length} to process only NEW records")
        save_last_offset(current_length)
        return current_length
    else:
        print(f"File has {current_length} records, resuming from offset {saved_offset}")
        return saved_offset

def save_last_offset(offset):
    # Save the current offset to a file and not memory
    try:
        with open(CONFIG['offsetFile'], 'w') as f:
            f.write(str(offset))
    except IOError as e:
        print(f"Warning: Could not save offset: {e}")


# Main simulation loop using loaded configuration
def simulate_machine():
    system_id = CONFIG['systemId']
    auth_token = CONFIG['jwtToken']
    csv_file_path = CONFIG['csvFilePath']
    send_delay = CONFIG['sendDelay']
    ingestion_url = "http://localhost:8081/api/systems/ingest" 

    last_processed_offset = initialize_offset()
    
    while True:
        try:
            if not os.path.exists(csv_file_path):
                print(f"Waiting for file: {csv_file_path}")
                time.sleep(1)
                continue

            current_file_length = get_current_file_length()
            
            if current_file_length < last_processed_offset:
                print(f"FILE RESET DETECTED DURING RUNTIME!")
                last_processed_offset = 0
                save_last_offset(0)
                continue
            
            if current_file_length <= last_processed_offset:
                print(f"No new records. File length: {current_file_length}, Last processed: {last_processed_offset}")
                time.sleep(1)
                continue
            
            new_records_count = current_file_length - last_processed_offset
            print(f"Found {new_records_count} new record(s). Processing from offset {last_processed_offset + 1}...")
            
            with open(csv_file_path, newline='', encoding='latin1') as csvfile:
                reader = csv.DictReader(csvfile)
                
                for i in range(last_processed_offset):
                    try:
                        next(reader)
                    except StopIteration:
                        break
                
                records_processed = 0
                for row_num, row in enumerate(reader, start=last_processed_offset + 1):
                    timestamp = row.get('Date/Time')

                    if not timestamp or not timestamp.strip():
                        print(f"Row {row_num}: Skipping row with missing 'Date/Time' field")
                        continue

                    metrics = {"timestamp": timestamp}
                    metrics_found = 0
                    
                    for csv_key, json_key in COLUMN_MAPPING.items():
                        value = row.get(csv_key)
                        if value and value.strip():
                            try:
                                metrics[json_key] = float(value)
                                metrics_found += 1
                            except ValueError:
                                metrics[json_key] = None
                    
                    payload = {
                        "systemId": system_id,
                        "authToken": auth_token,
                        "metricsPayload": metrics
                    }
                    headers = {"Authorization": f"Bearer {auth_token}"}

                    try:
                        response = requests.post(ingestion_url, json=payload, headers=headers, timeout=10)
                        print(f"[{system_id}] Row {row_num}: {response.status_code} - {response.text[:100]}...")
                    except requests.exceptions.RequestException as e:
                        print(f"[{system_id}] Row {row_num}: REQUEST ERROR: {e}")

                    last_processed_offset = row_num
                    save_last_offset(last_processed_offset)
                    records_processed += 1
                    time.sleep(send_delay)

            print(f"Finished processing {records_processed} new records. Waiting for more data...")
            # time.sleep(1)

        except Exception as e:
            print(f"[{CONFIG.get('systemId', 'UNKNOWN')}] ERROR: {e}")
            traceback.print_exc()
            time.sleep(2)

# ===================================================== MAIN EXECUTION ============================================================
if __name__ == "__main__":
    CONFIG = load_config()

    print("="*50)
    print("PULSE STACK SIMULATION CLIENT STARTING")
    print(f"System Name: {CONFIG.get('systemName', 'N/A')}")
    print(f"System ID: {CONFIG['systemId']}")
    print(f"CSV File: {CONFIG['csvFilePath']}")
    print(f"Send Delay: {CONFIG['sendDelay']} seconds between records")
    print(f"Offset File: {CONFIG['offsetFile']}")

    if os.path.exists(CONFIG['csvFilePath']):
        file_size = os.path.getsize(CONFIG['csvFilePath'])
        current_records = get_current_file_length()
        print(f"✓ CSV file found, size: {file_size} bytes, records: {current_records}")
    else:
        print(f"✗ CSV file NOT found: {CONFIG['csvFilePath']}")

    try:
        with open(CONFIG['csvFilePath'], 'r', encoding='latin1') as f:
            reader = csv.DictReader(f)
            headers = list(reader.fieldnames)
            found_cols = [col for col in COLUMN_MAPPING.keys() if col in headers]
            print(f"✓ Found {len(found_cols)} of {len(COLUMN_MAPPING)} expected columns")
    except Exception as e:
        print(f"✗ Error reading CSV: {e}")

    print("="*50)
    simulate_machine()