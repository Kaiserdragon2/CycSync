# CycSync File Transfer Script

## Overview

This script syncronizes all '.fit' files found on the target device(Cycplus M2). 
It might be possible to adapt this to other devices from Cycplus or Xoss by simply changing the target device name(`TARGET_NAME == Your_device_name`).

## Requirements

- Python 3.7+
- [Bleak](https://github.com/hbldh/bleak) library
- `asyncio` for asynchronous operations
- `logging` for logging information and errors

## Installation

1. Install the required dependencies:
    ```bash
    pip install bleak
    ```

## Usage

1. Run the script:
    ```bash
    python bluetooth_file_transfer.py
    ```
