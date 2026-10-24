# from pathlib import Path
# import math
#
# import joblib
# import numpy as np
# import pandas as pd
# from fastapi import FastAPI, HTTPException
# from pydantic import BaseModel, Field
#
#
# # --------------------------------------------------
# # 1. File paths and configuration
# # --------------------------------------------------
#
# BASE_DIR = Path(__file__).resolve().parent
#
# MODEL_PATH = BASE_DIR / "corrected_safety_model.pkl"
# DATASET_PATH = BASE_DIR / "crimedataset.csv"
#
# RADIUS_METERS = 500
# RADIUS_KM = RADIUS_METERS / 1000
# EARTH_RADIUS_KM = 6371.0
#
# ROUTE_SAMPLING_INTERVAL_METERS = 250
#
# # Provisional threshold: routes below this coverage will not
# # receive a Safe, Moderate, or Unsafe route-level label.
# # Validate this threshold against your dataset before deployment.
# MIN_DATA_COVERAGE_PERCENT = 70.0
#
# app = FastAPI(
#     title="TouristSafe Safety Prediction API",
#     description=(
#         "Estimates location and route safety scores using "
#         "a trained model and available crime records. "
#         "Routes with insufficient data coverage are explicitly "
#         "labelled Insufficient data."
#     ),
#     version="1.2.0",
# )
#
#
# # --------------------------------------------------
# # 2. Load the trained model and Excel dataset
# # --------------------------------------------------
#
# if not MODEL_PATH.exists():
#     raise FileNotFoundError(f"Model not found: {MODEL_PATH}")
#
# if not DATASET_PATH.exists():
#     raise FileNotFoundError(f"Dataset not found: {DATASET_PATH}")
#
# model_package = joblib.load(MODEL_PATH)
#
# model = model_package["model"]
# FEATURES = list(model_package["features"])
# SEVERITY_WEIGHTS = model_package["severity_weights"]
#
# CATEGORY_THRESHOLDS = model_package.get(
#     "category_thresholds",
#     {"safe": 75, "moderate": 50},
# )
#
# df = pd.read_csv(DATASET_PATH)
#
# required_columns = {"Latitude", "Longitude"}
# missing_columns = required_columns - set(df.columns)
#
# if missing_columns:
#     raise ValueError(
#         f"Missing coordinate columns: {sorted(missing_columns)}"
#     )
#
# CRIME_COLUMNS = list(SEVERITY_WEIGHTS.keys())
#
# missing_crime_columns = set(CRIME_COLUMNS) - set(df.columns)
#
# if missing_crime_columns:
#     raise ValueError(
#         "Dataset is missing crime columns: "
#         f"{sorted(missing_crime_columns)}"
#     )
#
# SUPPORTED_FEATURES = {
#     "crime_count_500m",
#     "crime_severity_500m",
#     "crime_density_500m",
#     "nearby_record_count",
# }
#
# if not FEATURES:
#     raise ValueError("The model package contains no features.")
#
# missing_features = set(FEATURES) - SUPPORTED_FEATURES
#
# if missing_features:
#     raise ValueError(
#         "Unsupported model features in model package: "
#         f"{sorted(missing_features)}"
#     )
#
# numeric_columns = list(
#     dict.fromkeys(CRIME_COLUMNS + ["Latitude", "Longitude"])
# )
#
# for column in numeric_columns:
#     df[column] = pd.to_numeric(df[column], errors="coerce")
#
# df = df.dropna(subset=numeric_columns).copy()
#
# df = df[
#     df["Latitude"].between(-90, 90)
#     & df["Longitude"].between(-180, 180)
# ].copy()
#
# if df.empty:
#     raise ValueError(
#         "No valid geographic crime records were found."
#     )
#
# # Crime counts should not be negative.
# if (df[CRIME_COLUMNS] < 0).any().any():
#     raise ValueError(
#         "Crime count columns contain negative values. "
#         "Check the Excel dataset before starting the API."
#     )
#
# print("TouristSafe model loaded successfully.")
# print(f"Dataset records available: {len(df)}")
# print(f"Model features: {FEATURES}")
# print(f"Search radius: {RADIUS_METERS} metres")
# print(
#     "Minimum route data coverage: "
#     f"{MIN_DATA_COVERAGE_PERCENT}%"
# )
#
#
# # --------------------------------------------------
# # 3. Request formats
# # --------------------------------------------------
#
# class LocationRequest(BaseModel):
#     latitude: float = Field(ge=-90, le=90)
#     longitude: float = Field(ge=-180, le=180)
#
#
# class RouteRequest(BaseModel):
#     # ORS coordinate order is [longitude, latitude].
#     # At least two coordinates are required.
#     coordinates: list[list[float]] = Field(min_length=2)
#
#
# # --------------------------------------------------
# # 4. Calculate geographic distances
# # --------------------------------------------------
#
# def haversine_distances(latitude, longitude, data):
#     """Return distances in metres from a point to dataset rows."""
#
#     lat1 = np.radians(latitude)
#     lon1 = np.radians(longitude)
#
#     lat2 = np.radians(
#         data["Latitude"].to_numpy(dtype=float)
#     )
#     lon2 = np.radians(
#         data["Longitude"].to_numpy(dtype=float)
#     )
#
#     delta_lat = lat2 - lat1
#     delta_lon = lon2 - lon1
#
#     a = (
#         np.sin(delta_lat / 2) ** 2
#         + np.cos(lat1)
#         * np.cos(lat2)
#         * np.sin(delta_lon / 2) ** 2
#     )
#
#     a = np.clip(a, 0.0, 1.0)
#
#     central_angle = 2 * np.arctan2(
#         np.sqrt(a),
#         np.sqrt(1.0 - a),
#     )
#
#     return EARTH_RADIUS_KM * central_angle * 1000
#
#
# # --------------------------------------------------
# # 5. Calculate model input features for one location
# # --------------------------------------------------
#
# def calculate_features(latitude, longitude):
#     distances = haversine_distances(
#         latitude,
#         longitude,
#         df,
#     )
#
#     nearby = df.loc[distances <= RADIUS_METERS]
#
#     # Never invent a prediction when no records are nearby.
#     if nearby.empty:
#         return None
#
#     crime_count = float(
#         nearby[CRIME_COLUMNS].to_numpy(dtype=float).sum()
#     )
#
#     crime_severity = 0.0
#
#     for crime in CRIME_COLUMNS:
#         crime_severity += (
#             float(nearby[crime].sum())
#             * float(SEVERITY_WEIGHTS[crime])
#         )
#
#     area_km2 = math.pi * (RADIUS_KM ** 2)
#     crime_density = crime_count / area_km2
#     nearby_record_count = int(len(nearby))
#
#     feature_values = {
#         "crime_count_500m": crime_count,
#         "crime_severity_500m": crime_severity,
#         "crime_density_500m": crime_density,
#         "nearby_record_count": nearby_record_count,
#     }
#
#     model_input = pd.DataFrame(
#         [[feature_values[name] for name in FEATURES]],
#         columns=FEATURES,
#     )
#
#     return feature_values, model_input
#
#
# # --------------------------------------------------
# # 6. Score and category helpers
# # --------------------------------------------------
#
# def predict_score(model_input):
#     """Run the saved model and constrain its output to 0–100."""
#
#     try:
#         prediction = float(model.predict(model_input)[0])
#     except Exception as exc:
#         raise HTTPException(
#             status_code=500,
#             detail="Safety model prediction failed.",
#         ) from exc
#
#     if not math.isfinite(prediction):
#         raise HTTPException(
#             status_code=500,
#             detail="The model returned an invalid score.",
#         )
#
#     return round(float(np.clip(prediction, 0, 100)), 2)
#
#
# def get_safety_category(score):
#     """Convert a numeric model score to its configured category."""
#
#     safe_threshold = float(
#         CATEGORY_THRESHOLDS.get("safe", 75)
#     )
#     moderate_threshold = float(
#         CATEGORY_THRESHOLDS.get("moderate", 50)
#     )
#
#     if score >= safe_threshold:
#         return "Safe", "Green"
#
#     if score >= moderate_threshold:
#         return "Moderate", "Yellow"
#
#     return "Unsafe", "Red"
#
#
# # --------------------------------------------------
# # 7. Sample points along a route
# # --------------------------------------------------
#
# def sample_route_coordinates(
#     coordinates,
#     spacing_m=ROUTE_SAMPLING_INTERVAL_METERS,
# ):
#     """
#     Sample route coordinates approximately every spacing_m.
#
#     Input and output coordinate order: [longitude, latitude].
#     """
#
#     if len(coordinates) < 2:
#         raise ValueError(
#             "A route must contain at least two coordinates."
#         )
#
#     if spacing_m <= 0:
#         raise ValueError(
#             "Sampling interval must be greater than zero."
#         )
#
#     validated_coordinates = []
#
#     for coordinate in coordinates:
#         if len(coordinate) != 2:
#             raise ValueError(
#                 "Each coordinate must contain exactly "
#                 "[longitude, latitude]."
#             )
#
#         longitude, latitude = coordinate
#
#         if not (
#             math.isfinite(longitude)
#             and math.isfinite(latitude)
#         ):
#             raise ValueError(
#                 "Coordinates must be finite numeric values."
#             )
#
#         if not (
#             -180 <= longitude <= 180
#             and -90 <= latitude <= 90
#         ):
#             raise ValueError(
#                 "Coordinate values are outside valid "
#                 "longitude/latitude ranges."
#             )
#
#         validated_coordinates.append(
#             [longitude, latitude]
#         )
#
#     sampled = []
#
#     for i in range(len(validated_coordinates) - 1):
#         lon1, lat1 = validated_coordinates[i]
#         lon2, lat2 = validated_coordinates[i + 1]
#
#         segment_data = pd.DataFrame({
#             "Latitude": [lat2],
#             "Longitude": [lon2],
#         })
#
#         distance = float(
#             haversine_distances(
#                 lat1,
#                 lon1,
#                 segment_data,
#             )[0]
#         )
#
#         steps = max(1, math.ceil(distance / spacing_m))
#
#         # Include the segment start and intermediate points.
#         # Each segment endpoint is included as the next segment's
#         # start, or as the final destination below.
#         for j in range(steps):
#             fraction = j / steps
#
#             longitude = lon1 + (lon2 - lon1) * fraction
#             latitude = lat1 + (lat2 - lat1) * fraction
#
#             sampled.append([longitude, latitude])
#
#     # Always include the destination.
#     sampled.append(validated_coordinates[-1])
#
#     return sampled
#
#
# # --------------------------------------------------
# # 8. Home endpoint
# # --------------------------------------------------
#
# @app.get("/")
# def home():
#     return {
#         "message": "TouristSafe Safety Prediction API is running.",
#         "docs": "/docs",
#         "health_endpoint": "/health",
#         "location_prediction_endpoint": "POST /predict",
#         "route_prediction_endpoint": "POST /predict-route",
#         "radius_meters": RADIUS_METERS,
#         "route_sampling_interval_meters": (
#             ROUTE_SAMPLING_INTERVAL_METERS
#         ),
#         "minimum_route_data_coverage_percent": (
#             MIN_DATA_COVERAGE_PERCENT
#         ),
#     }
#
#
# # --------------------------------------------------
# # 9. Health endpoint
# # --------------------------------------------------
#
# @app.get("/health")
# def health():
#     return {
#         "status": "healthy",
#         "model_loaded": model is not None,
#         "dataset_records": int(len(df)),
#         "radius_meters": RADIUS_METERS,
#         "minimum_route_data_coverage_percent": (
#             MIN_DATA_COVERAGE_PERCENT
#         ),
#     }
#
#
# # --------------------------------------------------
# # 10. Predict safety at one location
# # --------------------------------------------------
#
# @app.post("/predict")
# def predict_safety(request: LocationRequest):
#     result = calculate_features(
#         request.latitude,
#         request.longitude,
#     )
#
#     if result is None:
#         raise HTTPException(
#             status_code=422,
#             detail=(
#                 "No dataset records were found within "
#                 "500 metres of this location. A score cannot "
#                 "be estimated from the available local records."
#             ),
#         )
#
#     feature_values, model_input = result
#     safety_score = predict_score(model_input)
#     category, color = get_safety_category(safety_score)
#
#     return {
#         "latitude": request.latitude,
#         "longitude": request.longitude,
#         "search_radius_meters": RADIUS_METERS,
#         "safety_score": safety_score,
#         "category": category,
#         "color": color,
#         "features": {
#             "crime_count_500m": round(
#                 feature_values["crime_count_500m"], 2
#             ),
#             "crime_severity_500m": round(
#                 feature_values["crime_severity_500m"], 2
#             ),
#             "crime_density_500m": round(
#                 feature_values["crime_density_500m"], 2
#             ),
#             "nearby_record_count": feature_values[
#                 "nearby_record_count"
#             ],
#         },
#         "data_coverage": {
#             "nearby_records_found": True,
#             "records_within_500m": feature_values[
#                 "nearby_record_count"
#             ],
#         },
#         "limitations": (
#             "This is a dataset-based model estimate, not a "
#             "guarantee of actual or current safety. The available "
#             "dataset may not represent current conditions."
#         ),
#     }
#
#
# # --------------------------------------------------
# # 11. Predict safety along a complete route
# # --------------------------------------------------
#
# @app.post("/predict-route")
# def predict_route(request: RouteRequest):
#     try:
#         sampled_points = sample_route_coordinates(
#             request.coordinates
#         )
#     except (ValueError, TypeError, OverflowError) as exc:
#         raise HTTPException(
#             status_code=422,
#             detail=str(exc),
#         ) from exc
#
#     scored_values = []
#     scored_points = []
#     uncovered_points = 0
#
#     for longitude, latitude in sampled_points:
#         result = calculate_features(
#             latitude,
#             longitude,
#         )
#
#         # No nearby records: do not invent a point score.
#         if result is None:
#             uncovered_points += 1
#             continue
#
#         feature_values, model_input = result
#         score = predict_score(model_input)
#         point_category, point_color = get_safety_category(score)
#
#         scored_values.append(score)
#
#         scored_points.append({
#             "longitude": longitude,
#             "latitude": latitude,
#             "safety_score": score,
#             "category": point_category,
#             "color": point_color,
#             "nearby_records": feature_values[
#                 "nearby_record_count"
#             ],
#         })
#
#     total_points = len(sampled_points)
#     scored_count = len(scored_values)
#
#     if scored_count == 0:
#         raise HTTPException(
#             status_code=422,
#             detail={
#                 "message": (
#                     "No sampled route points had sufficient "
#                     "dataset coverage within 500 metres. "
#                     "No route safety score can be estimated."
#                 ),
#                 "total_sampled_points": total_points,
#                 "scored_points": 0,
#                 "uncovered_points": uncovered_points,
#                 "data_coverage_percent": 0.0,
#             },
#         )
#
#     # Average of covered points only. Uncovered points are not
#     # treated as safe or unsafe.
#     average_score = round(
#         float(np.mean(scored_values)),
#         2,
#     )
#
#     worst_score = round(
#         float(np.min(scored_values)),
#         2,
#     )
#
#     data_coverage_percent = round(
#         scored_count / total_points * 100,
#         2,
#     )
#
#     coverage_sufficient = (
#         data_coverage_percent >= MIN_DATA_COVERAGE_PERCENT
#     )
#
#     # Critical correction: never label a low-coverage route Safe,
#     # Moderate, or Unsafe. Its evidence is insufficient.
#     if not coverage_sufficient:
#         route_category = "Insufficient data"
#         route_color = "Gray"
#     else:
#         route_category, route_color = get_safety_category(
#             average_score
#         )
#
#     # Report the worst observed sampled point separately.
#     # This does not change the average route category.
#     worst_category, worst_color = get_safety_category(
#         worst_score
#     )
#
#     if not coverage_sufficient:
#         coverage_message = (
#             "Insufficient data coverage to classify this route. "
#             "The numeric score only reflects covered points."
#         )
#     else:
#         coverage_message = (
#             "Coverage meets the configured minimum threshold. "
#             "The score remains a dataset-based estimate, not "
#             "a guarantee of actual or current safety."
#         )
#
#     return {
#         "route_safety_score": average_score,
#         "worst_sampled_point_score": worst_score,
#         "worst_sampled_point_category": worst_category,
#         "worst_sampled_point_color": worst_color,
#         "category": route_category,
#         "color": route_color,
#         "coverage_sufficient": coverage_sufficient,
#         "minimum_required_coverage_percent": (
#             MIN_DATA_COVERAGE_PERCENT
#         ),
#         "sampling_interval_meters": (
#             ROUTE_SAMPLING_INTERVAL_METERS
#         ),
#         "search_radius_meters": RADIUS_METERS,
#         "total_sampled_points": total_points,
#         "scored_points": scored_count,
#         "uncovered_points": uncovered_points,
#         "data_coverage_percent": data_coverage_percent,
#         "points": scored_points,
#         "coverage_message": coverage_message,
#         "limitations": (
#             "This is a dataset-based estimate, not a guarantee "
#             "of actual or current safety. Uncovered points are "
#             "excluded from the score and are not considered safe "
#             "or unsafe. The route score is the simple mean of "
#             "covered sampled points and is not distance-weighted. "
#             "A high score with low coverage must not be interpreted "
#             "as evidence that the entire route is safe."
#         ),
#     }
from pathlib import Path
import os
import requests
import joblib
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, model_validator
# OpenRouteService configuration
ORS_API_KEY = os.getenv("ORS_API_KEY")

ORS_DIRECTIONS_URL = (
    "https://api.openrouteservice.org/v2/"
    "directions/driving-car/geojson"
)
SPRING_SAFETY_URL = os.getenv(
    "SPRING_SAFETY_URL",
    "https://touristappbackendspirngbooot.onrender.com/api/safety/predict"
)

app = FastAPI()
app = FastAPI(
    title="Tourist Safety ML Service",
    description="Predicts a safety score using nearby crime data",
    version="1.0.0"
)

# Load the saved model package
BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "corrected_safety_model.pkl"

try:
    model_data = joblib.load(MODEL_PATH)

    # The pickle file contains a dictionary
    model = model_data["model"]
    features = model_data["features"]

    print("Model loaded successfully.")
    print("Expected model features:", features)

except Exception as exc:
    raise RuntimeError(
        f"Could not load the trained model: {exc}"
    ) from exc


# Input received from Spring Boot
class SafetyFeatures(BaseModel):
    crime_count_500m: float = Field(ge=0)
    crime_severity_500m: float = Field(ge=0)
    crime_density_500m: float = Field(ge=0)
    nearby_record_count: float = Field(ge=0)


class MultipleRoutesRequest(BaseModel):
    origin_latitude: float = Field(ge=-90, le=90)
    origin_longitude: float = Field(ge=-180, le=180)
    destination_latitude: float = Field(ge=-90, le=90)
    destination_longitude: float = Field(ge=-180, le=180)

    @model_validator(mode="after")
    def validate_coordinates(self):
        if (
            self.origin_latitude == -90
            and self.origin_longitude == -180
        ):
            raise ValueError("Origin coordinates are placeholders.")

        if (
            self.destination_latitude == -90
            and self.destination_longitude == -180
        ):
            raise ValueError("Destination coordinates are placeholders.")

        if (
            self.origin_latitude == self.destination_latitude
            and self.origin_longitude == self.destination_longitude
        ):
            raise ValueError(
                "Origin and destination must be different."
            )

        return self

@app.get("/")
def home():
    return {
        "message": "Tourist Safety ML Service is running",
        "model_loaded": True
    }


@app.get("/health")
def health():
    return {
        "status": "UP",
        "model_loaded": model is not None
    }


@app.post("/predict")
def predict_safety(data: SafetyFeatures):
    try:
        # Match incoming values to the saved feature names
        feature_values = {
            "crime_count_500m": data.crime_count_500m,
            "crime_severity_500m": data.crime_severity_500m,
            "crime_density_500m": data.crime_density_500m,
            "nearby_record_count": data.nearby_record_count
        }

        # Preserve the feature order used during training
        input_data = np.array(
            [[feature_values[name] for name in features]],
            dtype=float
        )

        prediction = model.predict(input_data)
        safety_score = float(prediction[0])

        if not np.isfinite(safety_score):
            raise ValueError("The model returned an invalid score.")

        if not 0 <= safety_score <= 100:
            raise ValueError(
                f"Predicted score outside 0-100: {safety_score}"
            )

        return {
            "safety_score": safety_score
        }

    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=f"Safety prediction failed: {str(exc)}"
        ) from exc


import math



def score_route_points(coordinates, sample_limit=20):
    """
    Coordinates from GeoJSON are [longitude, latitude].
    Ask Spring Boot to score sampled points using existing logic.
    """
    if not coordinates:
        raise ValueError("Route has no coordinates.")

    # Select evenly spaced points along the route.
    count = len(coordinates)
    sample_count = min(sample_limit, count)

    if sample_count == 1:
        indices = [0]
    else:
        indices = sorted({
            round(i * (count - 1) / (sample_count - 1))
            for i in range(sample_count)
        })

    scores = []

    for index in indices:
        longitude, latitude = coordinates[index]

        response = requests.post(
            SPRING_SAFETY_URL,
            json={
                "latitude": latitude,
                "longitude": longitude
            },
            timeout=30
        )

if not response.ok:
    try:
        ors_error = response.json()
    except ValueError:
        ors_error = {"message": response.text[:1000]}

    error_details = ors_error.get("error", {})
    error_code = error_details.get("code")
    error_message = error_details.get(
        "message", "Unknown routing error"
    )

    if error_code == 2010:
        raise HTTPException(
            status_code=422,
            detail=(
                "ORS could not find a routable road near the "
                "origin or destination. Try coordinates closer "
                "to a road. ORS says: " + error_message
            ),
        )

    raise HTTPException(
        status_code=502,
        detail={
            "message": "OpenRouteService routing failed",
            "ors_status": response.status_code,
            "ors_error": ors_error,
        },
    )

result = response.json()


        score = float(result["safety_score"])

        if not math.isfinite(score) or not 0 <= score <= 100:
            raise ValueError(f"Invalid safety score: {score}")

        scores.append({
            "latitude": latitude,
            "longitude": longitude,
            "safety_score": score,
            "safety_category": result.get(
                "safety_category", "Unknown"
            )
        })

    average_score = sum(
        point["safety_score"] for point in scores
    ) / len(scores)

    if average_score >= 75:
        category = "Safe"
    elif average_score >= 50:
        category = "Moderate"
    else:
        category = "High Risk"

    return {
        "safety_score": round(average_score, 2),
        "safety_category": category,
        "sampled_points": scores
    }


@app.post("/routes")
def get_alternative_routes(data: MultipleRoutesRequest):
    if not ORS_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="ORS_API_KEY environment variable is not set."
        )

    payload = {
        "coordinates": [
            [data.origin_longitude, data.origin_latitude],
            [data.destination_longitude, data.destination_latitude]
        ],
        "alternative_routes": {
            "target_count": 3,
            "share_factor": 0.6,
            "weight_factor": 1.4
        }
    }

    headers = {
        "Authorization": ORS_API_KEY,
        "Content-Type": "application/json"
    }


    try:
        response = requests.post(
            ORS_DIRECTIONS_URL,
            json=payload,
            headers=headers,
            timeout=30
        )
        print("ORS URL:", ORS_DIRECTIONS_URL)
        print("ORS STATUS:", response.status_code)
        print("ORS RESPONSE:", response.text[:2000])
        response.raise_for_status()
        result = response.json()



        routes = []

        for index, feature in enumerate(result.get("features", [])):
            properties = feature.get("properties", {})
            summary = properties.get("summary", {})
            geometry = feature.get("geometry", {})
            coordinates = geometry.get("coordinates", [])

            try:
                safety = score_route_points(coordinates)
            except requests.RequestException as exc:
                raise HTTPException(
                    status_code=502,
                    detail=f"Could not score route {index + 1}: {exc}"
                ) from exc
            except (ValueError, KeyError, TypeError) as exc:
                raise HTTPException(
                    status_code=500,
                    detail=f"Route {index + 1} scoring failed: {exc}"
                ) from exc

            routes.append({
                "route_id": index + 1,
                "distance_meters": summary.get("distance"),
                "duration_seconds": summary.get("duration"),
                "geometry": geometry,
                "safety_score": safety["safety_score"],
                "safety_category": safety["safety_category"],
                "sampled_points": safety["sampled_points"]
            })

        if not routes:
            raise HTTPException(
                status_code=502,
                detail="ORS returned no routes."
            )

        return {"routes": routes}

    except requests.RequestException as exc:
        raise HTTPException(
            status_code=502,
            detail=f"OpenRouteService request failed: {exc}"
        ) from exc

