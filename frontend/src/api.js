import axios from 'axios';

const instance = axios.create({
  baseURL: process.env.VUE_APP_BACKEND_URL,
  withCredentials: true, // Send cookies with requests
});

// Request Interceptor: Attach access token to headers
instance.interceptors.request.use(
  (config) => {
    // If skipAuth is true, do not attach the Authorization header
    if (config.skipAuth) {
      return config;
    }

    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Handle 401 errors and token re-issue
instance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Handle authentication errors (401)
    // It's crucial to check for `error.response` to handle network errors where the server doesn't respond.
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true; // Mark request as retried to prevent infinite loops

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          // If there's no refresh token, the user needs to log in again.
          console.error('No refresh token found. Dispatching relogin-required event.');
          window.dispatchEvent(new Event('relogin-required'));
          return Promise.reject(error);
        }

        // Attempt to get a new access token using the refresh token
        const response = await axios.post(
          `${process.env.VUE_APP_BACKEND_URL}/api/v1/users/reissue`,
          { refreshToken: refreshToken },
          { withCredentials: true }
        );

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;

        // Update tokens in localStorage for future requests
        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        // Update the authorization header for the original request and retry it
        instance.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
        return instance(originalRequest);

      } catch (reissueError) {
        console.error('Token re-issue failed:', reissueError);
        // If re-issuing the token fails (e.g., refresh token is also expired),
        // clear all session data and force a re-login.
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userName');
        localStorage.removeItem('userProfileImage');
        window.dispatchEvent(new Event('relogin-required'));
        return Promise.reject(reissueError);
      }
    }

    // For all other errors (including network errors), just pass them along.
    return Promise.reject(error);
  }
);

export default instance;