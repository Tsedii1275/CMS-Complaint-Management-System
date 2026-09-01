const API_BASE_URL = 'http://localhost:8080';

const HTTP_ERROR_MESSAGES = {
  400: 'Invalid request parameters or payload.',
  401: 'Authentication token invalid or expired.',
  403: 'Access denied.',
  404: 'The requested resource or link was not found.',
  409: 'This request has already been submitted or completed.',
};

function ignoredAsNull(error) {
  void error;
  return null;
}

function parseJsonOrNull(bodyText) {
  try {
    return JSON.parse(bodyText);
  } catch (error) {
    return ignoredAsNull(error);
  }
}

function parseBodyIfJson(bodyText, contentType) {
  const trimmed = bodyText.trim();
  const looksLikeJson = contentType.includes('application/json')
    || trimmed.startsWith('{')
    || trimmed.startsWith('[');
  return looksLikeJson ? parseJsonOrNull(bodyText) : null;
}

async function readResponsePayload(response) {
  const contentType = response.headers.get('content-type') || '';
  let bodyText = '';
  let bodyData = null;
  try {
    bodyText = await response.text();
    bodyData = parseBodyIfJson(bodyText, contentType);
  } catch (error) {
    bodyData = ignoredAsNull(error);
  }
  return { bodyText, bodyData };
}

function fallbackHttpMessage(status, bodyText) {
  if (HTTP_ERROR_MESSAGES[status]) {
    return HTTP_ERROR_MESSAGES[status];
  }
  if (status >= 500) {
    return 'A temporary server error occurred. Please try again later.';
  }
  const useBody = bodyText && !bodyText.includes('<!DOCTYPE') && !bodyText.startsWith('Internal');
  return useBody ? bodyText : `Request failed with status ${status}`;
}

function withQuery(path, queryString) {
  return queryString ? `${path}?${queryString}` : path;
}

class ApiService {
  getAttachmentUrl(url) {
    if (!url) return '';
    // If it starts with localhost:8080, replace it with the configured API_BASE_URL
    if (url.startsWith('http://localhost:8080')) {
      return url.replace('http://localhost:8080', API_BASE_URL);
    }
    return url;
  }

  // Centralized Standardized Attachment Methods
  async uploadAttachment(file, complaintId, uploadedBy) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('complaintId', complaintId);
    if (uploadedBy) formData.append('uploadedBy', uploadedBy);

    const headers = {};
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user.token) headers['Authorization'] = `Bearer ${user.token}`;
    }

    const response = await fetch(`${API_BASE_URL}/api/attachments/upload`, {
      method: 'POST',
      headers,
      body: formData,
    });
    if (!response.ok) throw new Error('Attachment upload failed');
    return response.json();
  }

  async getAttachments(complaintId) {
    return this.get(`/api/attachments/complaint/${complaintId}`);
  }

  async deleteAttachment(attachmentId) {
    return this.delete(`/api/attachments/${attachmentId}`);
  }

  getHeaders() {
    const headers = {
      'Content-Type': 'application/json',
    };
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user.token) {
        headers['Authorization'] = `Bearer ${user.token}`;
      }
    }
    return headers;
  }

  async handleResponse(response) {
    const { bodyText, bodyData } = await readResponsePayload(response);

    if (!response.ok) {
      const code = bodyData?.code || `HTTP_${response.status}`;
      const message = bodyData?.message || bodyData?.error || fallbackHttpMessage(response.status, bodyText);
      const err = new Error(message);
      err.status = response.status;
      err.code = code;
      err.data = bodyData;
      throw err;
    }

    return bodyData !== null ? bodyData : { success: true };
  }

  async post(url, data) {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
    return this.handleResponse(response);
  }

  async get(url) {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      headers: this.getHeaders(),
    });
    return this.handleResponse(response);
  }

  async delete(url) {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });
    return this.handleResponse(response);
  }

  // Authentication
  async login(username, password) {
    return this.post('/api/auth/login', { username, password });
  }

  async updatePassword(currentPassword, newPassword, confirmPassword) {
    return this.put('/api/auth/password', { currentPassword, newPassword, confirmPassword });
  }

  // Submit new complaint
  async submitComplaint(complaintData) {
    return this.post('/api/complaints/start', complaintData);
  }

  // Check complaint status by ticket number
  async checkComplaintStatus(ticketId) {
    const cleanTicket = (ticketId || '').trim();
    return this.get(`/api/complaints/status?ticketId=${encodeURIComponent(cleanTicket)}`);
  }

  // Staff submits a complaint (possibly already resolved)
  async staffSubmitComplaint(complaintData) {
    return this.post('/api/complaints/staff-submit', complaintData);
  }

  // Staff resolves complaint at First Contact Resolution (no workflow started)
  async fcrResolveComplaint(complaintData) {
    return this.post('/api/complaints/fcr-resolve', complaintData);
  }

  // Close complaint
  async closeComplaint(id) {
    return this.post(`/api/complaints/${id}/close`, {});
  }

  // Get tasks for a specific role/group
  async getTasks(candidateGroup) {
    const params = candidateGroup ? `?candidateGroup=${candidateGroup}` : '';
    return this.get(`/api/tasks${params}`);
  }

  // Get enriched tasks with full details
  async getEnrichedTasks(candidateGroup) {
    const params = candidateGroup ? `?candidateGroup=${candidateGroup}` : '';
    return this.get(`/api/tasks/enriched${params}`);
  }

  // Complete a task with variables
  async completeTask(taskId, variables) {
    return this.post(`/api/tasks/${taskId}/complete`, variables);
  }

  // Reject FCR status (keeps task open in screening queue)
  async rejectFcr(taskId) {
    return this.post(`/api/process/fcr/reject/${taskId}`, {});
  }

  // Submit Audit Result
  async submitAuditResult(taskId, variables) {
    return this.completeTask(taskId, variables);
  }

  // Get task variables
  async getTaskVariables(taskId) {
    return this.get(`/api/tasks/${taskId}/variables`);
  }

  // Delete a process instance
  async deleteProcessInstance(instanceId) {
    const response = await fetch(`${API_BASE_URL}/api/process/${instanceId}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return true;
  }

  // Clear all tasks and process instances from database
  async clearAllTasks() {
    return this.post('/api/process/clear-all', {});
  }

  // Fetch First Contact Resolution (FCR) database records
  async getFcrRecords() {
    return this.get('/api/process/fcr/all');
  }

  // Get audit logs with optional filters
  async getAuditLogs(filters = {}) {
    const queryParams = new URLSearchParams();

    if (filters.action) queryParams.append('action', filters.action);
    if (filters.complaintId) queryParams.append('complaintId', filters.complaintId);
    if (filters.actor) queryParams.append('actor', filters.actor);
    if (filters.startDate) queryParams.append('startDate', filters.startDate);
    if (filters.endDate) queryParams.append('endDate', filters.endDate);

    const queryString = queryParams.toString();
    return this.get(withQuery('/api/audit/logs', queryString));
  }

  // Get all SLA metrics
  async getAllSlaMetrics() {
    return this.get('/api/audit/sla/all');
  }

  async getOtherSlaMetrics() {
    return this.get('/api/audit/sla/other');
  }

  // Get SLA report for a specific complaint by process instance ID
  async getSlaReport(processInstanceId) {
    return this.get(`/api/audit/sla/process/${processInstanceId}`);
  }

  // Get SLA report by complaint/ticket ID
  async getSlaByComplaintId(complaintId) {
    return this.get(`/api/audit/sla/complaint/${complaintId}`);
  }

  // Get task time tracking for a process instance
  async getTaskTracking(processInstanceId) {
    return this.get(`/api/audit/sla/tasks/${processInstanceId}`);
  }

  // Assign or Reassign Task to a specific Customer Care Officer
  async assignTask(taskId, targetUsername) {
    return this.post(`/api/tasks/${taskId}/assign?targetUsername=${encodeURIComponent(targetUsername)}`, {});
  }

  // Update and persist complaint record details
  async updateComplaintDetails(taskId, payload) {
    return this.post(`/api/complaints/${taskId}/update-details`, payload);
  }

  // Get active Customer Care Officers list
  async getCustomerCareOfficers() {
    return this.get('/api/users/officers');
  }

  // Get Complaint Stage SLA Timeline
  async getComplaintSlaTimeline(complaintId) {
    if (!complaintId) return [];
    const encoded = encodeURIComponent(complaintId);
    return this.get(`/api/complaints/timeline?complaintId=${encoded}`);
  }

  // ─── SLA Governance & Configuration ───
  async getSlaConfigs() {
    return this.get('/api/sla/config');
  }

  async updateSlaConfig(id, data) {
    return this.put(`/api/sla/config/${id}`, data);
  }

  async resetSlaConfigs() {
    return this.post('/api/sla/config/reset', {});
  }

  async getOperatingHours() {
    return this.get('/api/sla/config/operating-hours');
  }

  async getHolidays() {
    return this.get('/api/sla/config/holidays');
  }

  async addHoliday(data) {
    return this.post('/api/sla/config/holidays', data);
  }

  async deleteHoliday(id) {
    return this.delete(`/api/sla/config/holidays/${id}`);
  }

  // Get Districts, Branches, and Departments hierarchy
  async getHierarchy() {
    return this.get('/api/hierarchy');
  }

  // Upload audio file/blob
  async uploadAudio(file, fileName = 'recording.wav') {
    const formData = new FormData();
    formData.append('file', file, fileName);

    const headers = {};
    const userStr = localStorage.getItem('user');
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user.token) {
        headers['Authorization'] = `Bearer ${user.token}`;
      }
    }

    const response = await fetch('http://localhost:8080/api/complaints/upload-audio', {
      method: 'POST',
      headers: headers,
      body: formData
    });

    if (!response.ok) {
      throw new Error(`Upload failed! status: ${response.status}`);
    }

    return response.json();
  }

  // Validate customer feedback token
  async validateCustomerFeedbackToken(token) {
    const response = await fetch(`${API_BASE_URL}/api/customer-feedback/validate?token=${token}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || data.error || `HTTP error! status: ${response.status}`);
    }
    return data;
  }

  // Submit customer resolution feedback (public, no auth needed)
  async submitCustomerFeedback(payload) {
    const response = await fetch(`${API_BASE_URL}/api/customer-feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || data.error || `HTTP error! status: ${response.status}`);
    }
    return data;
  }

  // Upload evidence file (public, no auth needed)
  async uploadEvidence(file) {
    const formData = new FormData();
    formData.append('file', file, file.name);

    const response = await fetch(`${API_BASE_URL}/api/complaints/upload-evidence`, {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`Upload failed! status: ${response.status}`);
    }

    return response.json();
  }

  // Get NBE Reports Data
  async getNbeReportsData() {
    return this.get('/api/complaints/nbe-reports');
  }

  // NBE compliance report persistence (nbe_compliance_reports table)
  async getNbeComplianceReports() {
    return this.get('/api/nbe-compliance-reports');
  }

  async saveNbeComplianceReport(payload) {
    return this.put('/api/nbe-compliance-reports', payload);
  }

  async deleteNbeComplianceReport(ticketNumber) {
    return this.delete(`/api/nbe-compliance-reports?ticketNumber=${encodeURIComponent(ticketNumber)}`);
  }

  // RCA Module APIs
  async triggerRca(ticketId, processInstanceId, rcaRequired) {
    return this.post('/api/rca/trigger', { ticketId, processInstanceId, rcaRequired });
  }

  async getRcaCases() {
    return this.get('/api/rca/cases');
  }

  async getRcaCaseById(id) {
    return this.get(`/api/rca/cases/${id}`);
  }

  async updateRcaCase(id, payload) {
    const response = await fetch(`${API_BASE_URL}/api/rca/cases/${id}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }

  async updateRcaWhys(id, payload) {
    const response = await fetch(`${API_BASE_URL}/api/rca/cases/${id}/whys`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }

  async addCapaAction(id, payload) {
    return this.post(`/api/rca/cases/${id}/capa`, payload);
  }

  async updateCapaAction(actionId, payload) {
    const response = await fetch(`${API_BASE_URL}/api/rca/capa/${actionId}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }

  async deleteCapaAction(actionId) {
    const response = await fetch(`${API_BASE_URL}/api/rca/capa/${actionId}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }

  async getRcaAuditLogs(id) {
    return this.get(`/api/rca/cases/${id}/audit-logs`);
  }

  async getRcaAnalytics() {
    return this.get('/api/rca/analytics');
  }

  async getRootCauseAnalysis(filters = {}) {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && value !== 'ALL') {
        params.append(key, value);
      }
    });
    const query = params.toString();
    return this.get(withQuery('/api/rca/root-cause-analysis', query));
  }

  async exportRootCauseAnalysis(format, filters = {}) {
    const params = new URLSearchParams();
    params.append('format', format || 'csv');
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && value !== 'ALL') {
        params.append(key, value);
      }
    });
    const ext = format === 'excel' ? 'xls' : 'csv';
    return this.downloadFile(`/api/rca/root-cause-analysis/export?${params.toString()}`, `Root_Cause_Analysis.${ext}`);
  }

  async getCustomerFeedbackAnalytics() {
    return this.get('/api/customer-feedback/analytics');
  }

  async getCustomerFeedbackDistributions() {
    return this.get('/api/customer-feedback/distributions');
  }

  async getCustomerFeedbackTrends() {
    return this.get('/api/customer-feedback/trends');
  }

  async getCustomerFeedbackList(query = '') {
    return this.get(`/api/customer-feedback/list?query=${encodeURIComponent(query)}`);
  }

  async getUnresolvedFollowups() {
    return this.get('/api/customer-feedback/unresolved-followups');
  }

  async startUnresolvedFollowup(id, username = '') {
    return this.post(`/api/customer-feedback/unresolved-followups/${id}/start?username=${encodeURIComponent(username)}`);
  }

  async closeUnresolvedFollowup(id, username = '') {
    return this.post(`/api/customer-feedback/unresolved-followups/${id}/close?username=${encodeURIComponent(username)}`);
  }

  buildQueryString(filters = {}) {
    const params = new URLSearchParams();
    Object.keys(filters).forEach(key => {
      if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') {
        params.append(key, filters[key]);
      }
    });
    return params.toString();
  }

  async getAnalyticsStats(filters) {
    const query = this.buildQueryString(filters);
    return this.get(`/api/audit/analytics/stats?${query}`);
  }

  async getAnalyticsTrend(interval, filters) {
    const query = this.buildQueryString(filters);
    return this.get(`/api/audit/analytics/trend?interval=${interval}&${query}`);
  }

  async getAnalyticsReports(filters) {
    const query = this.buildQueryString(filters);
    return this.get(`/api/audit/analytics/reports?${query}`);
  }

  async exportAnalyticsData(format, filters) {
    const query = this.buildQueryString(filters);
    const response = await fetch(`${API_BASE_URL}/api/audit/analytics/export?format=${format}&${query}`, {
      method: 'GET',
      headers: this.getHeaders()
    });
    if (!response.ok) {
      throw new Error(`Export failed: ${response.status}`);
    }
    return response.blob();
  }

  async put(url, data) {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
    return this.handleResponse(response);
  }

  // ─── User Management API Methods ───
  async getUsers() {
    return this.get('/api/admin/users');
  }

  async createUser(userData) {
    return this.post('/api/admin/users', userData);
  }

  async updateUser(id, userData) {
    return this.put(`/api/admin/users/${id}`, userData);
  }

  async toggleUserStatus(id) {
    return this.put(`/api/admin/users/${id}/toggle-status`, {});
  }

  async resetUserPassword(id, newPassword) {
    const pwd = (newPassword && String(newPassword).trim() !== '') ? String(newPassword).trim() : '123';
    return this.post(`/api/admin/users/${id}/reset-password`, { newPassword: pwd });
  }

  async deleteUser(id) {
    return this.delete(`/api/admin/users/${id}`);
  }

  // ─── CMD Analytics Methods ───
  async getCmdTeamWorkload() {
    return this.get('/api/cmd/analytics/team-workload');
  }

  async getCmdDepartmentPerformance() {
    return this.get('/api/cmd/analytics/department-performance');
  }

  async getCmdOfficerPerformance() {
    return this.get('/api/cmd/analytics/officer-performance');
  }

  async exportCmdReport() {
    return this.downloadFile('/api/cmd/analytics/export/csv', 'cmd_department_performance_report.csv');
  }

  async downloadFile(endpoint, filename) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'GET',
      headers: this.getHeaders()
    });
    if (!response.ok) {
      throw new Error(`Download failed: ${response.status}`);
    }

    // Extract filename from Content-Disposition if present
    let downloadFilename = filename;
    const disposition = response.headers.get('content-disposition');
    if (disposition?.includes('attachment')) {
      const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
      const matches = filenameRegex.exec(disposition);
      if (matches?.[1]) {
        downloadFilename = matches[1].replace(/['"]/g, '');
      }
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = downloadFilename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  }

  // ─── Complainant Related Information API Methods ───
  async getComplainantRelatedInformation(filters = {}) {
    const params = new URLSearchParams();
    if (filters.search) params.append('search', filters.search);
    if (filters.status) params.append('status', filters.status);
    if (filters.category) params.append('category', filters.category);
    if (filters.district) params.append('district', filters.district);
    if (filters.page !== undefined) params.append('page', filters.page);
    if (filters.size !== undefined) params.append('size', filters.size);
    const queryString = params.toString();
    return this.get(withQuery('/api/complainant-related-information', queryString));
  }

  async updateComplainantRelatedInformation(id, data) {
    return this.put(`/api/complainant-related-information/${id}`, data);
  }

  async deleteComplainantRelatedInformation(id) {
    return this.delete(`/api/complainant-related-information/${id}`);
  }

  async syncComplainantRelatedInformation() {
    return this.post('/api/complainant-related-information/sync', {});
  }
}

export default new ApiService();

